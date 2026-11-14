package td.ui;

import td.ui.render.TowerLook;
import td.ui.render.TowerPose;
import td.ui.render.TowerSpriteDraw;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Paints a tower in the raised view: a body of lit parts, the head it aims, a soft shadow on the ground and the lights
 * that keep it alive while it idles. A body is tens of parts, so each is drawn once into a picture per tier, pose and
 * zoom and kept; a head is kept per heading step too. Placing a picture is all a still tower costs a frame; only its
 * lights, a tier's rise and a tower settling onto its cell are painted live.
 */
final class TowerSprites {

    /** How far up the screen a part's top is lifted, as a share of its height: the board is seen from the south. */
    private static final double LIFT = 0.42;
    /** The lit rim along a part's top-left edge, in cells. */
    private static final double RIM = 0.024;
    /** A head turns in this many steps; between two of them it reuses the nearer picture. */
    private static final int FACINGS = 64;
    private static final int BODY = -1;
    private static final int FRONT = -2;
    private static final float DEVICE_STEPS_PER_UNIT = 16f;
    /** Past this many kept pixels every picture is dropped and drawn again as it is needed. */
    private static final long PIXEL_BUDGET = 12_000_000L;
    private static final int PADDING = 2;
    private static final Color SHADOW = new Color(0, 0, 0, 150);

    /** How far the new foundation has spread when the rise ends, and how far above the cell's centre it spreads from. */
    private static final float RISE_SPREAD_CELLS = 1.2f;
    private static final float RISE_SPREAD_LIFT_CELLS = 0.3f;
    /** The rise's ring starts inside the foundation and ends past the cell's edge. */
    private static final float RISE_RING_FROM_CELLS = 0.2f;
    private static final float RISE_RING_TO_CELLS = 0.8f;
    private static final float RISE_RING_STROKE_CELLS = 0.06f;
    /** The new foundation swells past its size as it pushes out, then settles. */
    private static final float RISE_SWELL = 0.12f;
    /** The rise opens with a flash that is gone by this share of it. */
    private static final float RISE_FLASH_SHARE = 0.35f;
    private static final float RISE_FLASH_ALPHA = 0.55f;
    private static final int RISE_SPARKS = 8;
    private static final float RISE_SPARK_FROM_CELLS = 0.18f;
    private static final float RISE_SPARK_TO_CELLS = 0.62f;
    private static final float RISE_SPARK_CLIMB_CELLS = 0.3f;
    private static final float RISE_SPARK_CELLS = 0.03f;

    /** A placed tower starts this small and overshoots by this much before it settles. */
    private static final float BUILD_FROM = 0.45f;
    private static final float BUILD_OVERSHOOT = 0.1f;
    /** It is fully opaque by this share of its settling. */
    private static final float BUILD_FADE_SHARE = 0.4f;
    private static final float BUILD_DUST_TO_CELLS = 0.62f;
    private static final float BUILD_DUST_ALPHA = 0.5f;
    private static final float BUILD_DUST_STROKE_CELLS = 0.02f;
    private static final Color BUILD_DUST = new Color(0xb9bcc8);

    private static final double BREATH_SECONDS = 3.4;
    private static final float BREATH_ALPHA = 0.34f;
    private static final float BREATH_ALPHA_SWING = 0.26f;
    private static final float BREATH_RADIUS_SWING = 0.16f;
    private static final float FLICKER_ALPHA = 0.5f;
    private static final double GLINT_SECONDS = 5.5;
    private static final double GLINT_SHOWS_SECONDS = 0.45;
    private static final double SWEEP_SECONDS = 7.0;
    private static final float SWEEP_ALPHA = 0.75f;
    private static final double SWEEP_HALF_WIDTH = Math.toRadians(16);

    /** Where a head points in a still picture, and the room a tower takes in one, in cells. */
    private static final double STILL_HEADING = Math.toRadians(-35);
    private static final double STILL_ROOM_CELLS = 2.0;
    private static final double DRAINED_LIGHT = 0.62;
    private static final double DRAINED_ALPHA = 0.75;

    private static final Map<Key, Sprite> SPRITES = new ConcurrentHashMap<>();
    private static final AtomicLong KEPT_PIXELS = new AtomicLong();

    private TowerSprites() {
    }

    /** What a picture shows: a look at a tier in a pose. */
    private record Posture(TowerLook look, int tier, TowerPose pose) {
    }

    /**
     * {@code layer} is a head's heading step, or the body or its front; {@code pixelSteps} is the device pixels to a
     * cell, in sixteenths, so a zoom or a HiDPI scale gets its own.
     */
    private record Key(Posture posture, int layer, int pixelSteps) {
    }

    /** A picture and where the tower's centre sits in it, in device pixels. */
    private record Sprite(BufferedImage image, int centreX, int centreY) {
    }

    /** The board's transform, and whether it only scales and moves, so a picture can be placed pixel for pixel. */
    private record Pen(AffineTransform board, boolean plain, float device) {

        static Pen of(Graphics2D g2) {
            AffineTransform board = g2.getTransform();
            boolean plain = board.getShearX() == 0 && board.getShearY() == 0 && board.getScaleX() > 0
                    && Math.abs(board.getScaleX() - board.getScaleY()) < 1e-6;
            float device = plain ? Math.round((float) board.getScaleX() * DEVICE_STEPS_PER_UNIT) / DEVICE_STEPS_PER_UNIT
                    : 1f;
            return new Pen(board, plain, Math.max(device, 1f / DEVICE_STEPS_PER_UNIT));
        }
    }

    /** The soft shadow a foundation casts on the ground, down and to the right; it grows as a placed tower settles. */
    static void paintShadow(Graphics2D g2, TowerSpriteDraw tower, int scale) {
        double footprint = TowerFoundations.footprint(tower.tier()) * scale * settled(tower.buildProgress());
        g2.setColor(SHADOW);
        g2.fill(new Ellipse2D.Double(tower.centerX() + footprint * 0.14 - footprint * 1.06,
                tower.centerY() + footprint * 0.16 - footprint * 1.02, footprint * 2.12, footprint * 2.04));
    }

    /**
     * The body, the head of a look that aims, and its idle lights. While its tier rises, the new foundation pushes out
     * of the old one from the centre, swelling past its size before it settles, under a flash, a ring in the new tier's
     * colour that runs out past the cell's edge, and sparks. A tower just placed grows out of its cell and settles.
     */
    static void paint(Graphics2D g2, TowerSpriteDraw tower, int scale) {
        float build = tower.buildProgress();
        float rise = tower.riseProgress();
        if (build < 1f) {
            paintSettling(g2, tower, scale, build);
        } else if (rise < 1f) {
            paintRising(g2, tower, scale, rise);
        } else {
            paintTower(g2, tower, tower.tier(), scale, 1f, 1f);
        }
        if (build >= 1f) {
            paintLights(g2, tower, scale);
        }
    }

    /** {@code look} at {@code tier} in {@code pose}, still: no lights, nothing rising. */
    static void paint(Graphics2D g2, TowerLook look, int tier, TowerPose pose, float centerX, float centerY, int scale,
                      double heading) {
        paintTower(g2, look, tier, pose, centerX, centerY, scale, heading, 1f, 1f);
    }

    /**
     * {@code look} at {@code tier} at rest, for a panel: centred on the pen, a cell {@code cell} wide. {@code drained}
     * takes its colour away, for a tier not reached yet.
     */
    static void paintStill(Graphics2D g2, TowerLook look, int tier, double cell, boolean drained) {
        AffineTransform pen = g2.getTransform();
        if (!drained) {
            g2.scale(cell, cell);
            paintTower(g2, look, tier, TowerPose.idle(), 0f, 0f, 1, STILL_HEADING, 1f, 1f);
            g2.setTransform(pen);
            return;
        }
        double device = cell * Math.max(1.0, pen.getScaleX());
        int side = (int) Math.ceil(device * STILL_ROOM_CELLS);
        BufferedImage picture = new BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = picture.createGraphics();
        g.translate(side / 2.0, side / 2.0);
        g.scale(device, device);
        paintTower(g, look, tier, TowerPose.idle(), 0f, 0f, 1, STILL_HEADING, 1f, 1f);
        g.dispose();
        drain(picture);
        g2.scale(cell / device, cell / device);
        g2.drawImage(picture, -side / 2, -side / 2, null);
        g2.setTransform(pen);
    }

    private static void drain(BufferedImage picture) {
        for (int y = 0; y < picture.getHeight(); y++) {
            for (int x = 0; x < picture.getWidth(); x++) {
                int argb = picture.getRGB(x, y);
                int grey = (int) ((0.3 * ((argb >> 16) & 0xff) + 0.59 * ((argb >> 8) & 0xff) + 0.11 * (argb & 0xff))
                        * DRAINED_LIGHT);
                int alpha = (int) ((argb >>> 24) * DRAINED_ALPHA);
                picture.setRGB(x, y, alpha << 24 | grey << 16 | grey << 8 | grey);
            }
        }
    }

    /**
     * Where a shot leaves {@code look} in {@code pose}, its head turned to {@code heading}, in board pixels: the head's
     * far end at its middle height, lifted as the head is drawn. A look with no head shoots from its top.
     */
    static Point2D.Float muzzle(TowerLook look, int tier, TowerPose pose, float centerX, float centerY, int scale,
                                double heading) {
        TowerFoundations.Foundation foundation = TowerFoundations.of(look, tier, pose);
        return new Point2D.Float((float) (centerX + Math.cos(heading) * foundation.muzzle() * scale),
                (float) (centerY + Math.sin(heading) * foundation.muzzle() * scale
                        - LIFT * foundation.muzzleHeight() * scale));
    }

    private static void paintRising(Graphics2D g2, TowerSpriteDraw tower, int scale, float progress) {
        float x = tower.centerX();
        float y = tower.centerY();
        Color tier = TowerLooks.tierColor(tower.look(), tower.tier());
        paintTower(g2, tower, tower.risenFrom(), scale, 1f, 1f);
        Shape clip = g2.getClip();
        float spread = scale * RISE_SPREAD_CELLS * progress;
        float spreadY = y - scale * RISE_SPREAD_LIFT_CELLS;
        g2.clip(new Ellipse2D.Float(x - spread, spreadY - spread, spread * 2, spread * 2));
        paintTower(g2, tower, tower.tier(), scale, 1f + RISE_SWELL * (float) Math.sin(Math.PI * progress), 1f);
        g2.setClip(clip);
        if (progress < RISE_FLASH_SHARE) {
            float fade = 1f - progress / RISE_FLASH_SHARE;
            float radius = scale * (float) TowerFoundations.footprint(tower.tier()) * 1.5f;
            float top = y - (float) (LIFT * TowerFoundations.of(tower.look(), tower.tier()).top() * scale * 0.5);
            glow(g2, x, top, radius, Color.WHITE, RISE_FLASH_ALPHA * fade);
        }
        float radius = scale * (RISE_RING_FROM_CELLS + (RISE_RING_TO_CELLS - RISE_RING_FROM_CELLS) * progress);
        Stroke stroke = g2.getStroke();
        g2.setStroke(new BasicStroke(scale * RISE_RING_STROKE_CELLS));
        g2.setColor(faded(tier, 0.9f * (1f - progress)));
        g2.draw(new Ellipse2D.Float(x - radius, y - radius, radius * 2, radius * 2));
        g2.setStroke(stroke);
        float eased = 1f - (1f - progress) * (1f - progress);
        float reach = scale * (RISE_SPARK_FROM_CELLS + (RISE_SPARK_TO_CELLS - RISE_SPARK_FROM_CELLS) * eased);
        float climb = scale * RISE_SPARK_CLIMB_CELLS * (float) Math.sin(Math.PI * progress);
        float size = scale * RISE_SPARK_CELLS * (1f - 0.5f * progress);
        g2.setColor(faded(mixed(tier, Color.WHITE, 0.4f), 1f - progress));
        for (int i = 0; i < RISE_SPARKS; i++) {
            double bearing = 2 * Math.PI * (i + 0.5) / RISE_SPARKS;
            float sx = x + (float) Math.cos(bearing) * reach;
            float sy = y + (float) Math.sin(bearing) * reach * 0.7f - climb;
            g2.fill(new Ellipse2D.Float(sx - size, sy - size, size * 2, size * 2));
        }
    }

    private static void paintSettling(Graphics2D g2, TowerSpriteDraw tower, int scale, float progress) {
        float footprint = scale * (float) TowerFoundations.footprint(tower.tier());
        float dust = footprint + (scale * BUILD_DUST_TO_CELLS - footprint) * progress;
        Stroke stroke = g2.getStroke();
        g2.setStroke(new BasicStroke(scale * (BUILD_DUST_STROKE_CELLS + 0.05f * (1f - progress))));
        g2.setColor(faded(BUILD_DUST, BUILD_DUST_ALPHA * (1f - progress)));
        g2.draw(new Ellipse2D.Float(tower.centerX() - dust, tower.centerY() - dust * 0.8f, dust * 2, dust * 1.6f));
        g2.setStroke(stroke);
        paintTower(g2, tower, tower.tier(), scale, settled(progress), Math.min(1f, progress / BUILD_FADE_SHARE));
    }

    /** A placed tower's size as it settles: it grows quickly, runs a little past its size and comes back. */
    private static float settled(float progress) {
        if (progress >= 1f) {
            return 1f;
        }
        float eased = 1f - (1f - progress) * (1f - progress);
        float over = BUILD_OVERSHOOT * (float) Math.sin(Math.PI * progress) * progress;
        return BUILD_FROM + (1f - BUILD_FROM) * eased + over;
    }

    private static void paintTower(Graphics2D g2, TowerSpriteDraw tower, int tier, int scale, float size, float alpha) {
        paintTower(g2, tower.look(), tier, tower.pose(), tower.centerX(), tower.centerY(), scale,
                tower.headingRadians(), size, alpha);
    }

    private static void paintTower(Graphics2D g2, TowerLook look, int tier, TowerPose pose, float centerX,
                                   float centerY, int scale, double heading, float size, float alpha) {
        Pen pen = Pen.of(g2);
        TowerPose snapped = pose.snapped(TowerFoundations.POSE_STEPS);
        TowerPose body = TowerFoundations.stillBody(look) ? TowerPose.idle() : snapped;
        place(g2, pen, sprite(look, tier, body, BODY, scale, pen.device()), centerX, centerY, size, alpha);
        TowerFoundations.Foundation foundation = TowerFoundations.of(look, tier, snapped);
        if (foundation.aims()) {
            int facing = Math.floorMod((int) Math.round(heading / (2 * Math.PI) * FACINGS), FACINGS);
            place(g2, pen, sprite(look, tier, snapped, facing, scale, pen.device()), centerX, centerY, size, alpha);
        }
        if (!foundation.front().isEmpty()) {
            place(g2, pen, sprite(look, tier, body, FRONT, scale, pen.device()), centerX, centerY, size, alpha);
        }
    }

    /** Device pixel for device pixel when the board only scales, so the picture stays as sharp as its shapes. */
    private static void place(Graphics2D g2, Pen pen, Sprite sprite, float x, float y, float size, float alpha) {
        Composite composite = g2.getComposite();
        if (alpha < 1f) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.clamp(alpha, 0f, 1f)));
        }
        AffineTransform board = pen.board();
        if (pen.plain()) {
            long deviceX = Math.round(board.getTranslateX() + board.getScaleX() * x);
            long deviceY = Math.round(board.getTranslateY() + board.getScaleY() * y);
            g2.setTransform(new AffineTransform());
            if (size == 1f) {
                g2.drawImage(sprite.image(), (int) deviceX - sprite.centreX(), (int) deviceY - sprite.centreY(), null);
            } else {
                g2.translate(deviceX, deviceY);
                g2.scale(size, size);
                g2.drawImage(sprite.image(), -sprite.centreX(), -sprite.centreY(), null);
            }
        } else {
            g2.translate(Math.round(x), Math.round(y));
            g2.scale(size, size);
            g2.drawImage(sprite.image(), -sprite.centreX(), -sprite.centreY(), null);
        }
        g2.setTransform(board);
        g2.setComposite(composite);
    }

    // ---- lights: the only part of an idle tower that is painted live ---------------------------------------------

    private static void paintLights(Graphics2D g2, TowerSpriteDraw tower, int scale) {
        List<TowerFoundations.Light> lights = TowerFoundations.of(tower.look(), tower.tier(), tower.pose()).lights();
        if (lights.isEmpty()) {
            return;
        }
        double cos = Math.cos(tower.headingRadians());
        double sin = Math.sin(tower.headingRadians());
        double seconds = tower.lifeSeconds();
        for (int i = 0; i < lights.size(); i++) {
            TowerFoundations.Light light = lights.get(i);
            double lx = light.turning() ? light.x() * cos - light.y() * sin : light.x();
            double ly = light.turning() ? light.x() * sin + light.y() * cos : light.y();
            float x = tower.centerX() + (float) (lx * scale);
            float y = tower.centerY() + (float) ((ly - LIFT * light.z()) * scale);
            float radius = (float) (light.radius() * scale);
            switch (light.life()) {
                case BREATH -> {
                    float breath = (float) Math.sin(2 * Math.PI * seconds / BREATH_SECONDS);
                    glow(g2, x, y, radius * (1f + BREATH_RADIUS_SWING * breath), light.color(),
                            BREATH_ALPHA + BREATH_ALPHA_SWING * breath);
                }
                case FLICKER -> {
                    float flick = (float) (0.5 * Math.sin(seconds * 23.0) + 0.3 * Math.sin(seconds * 37.7 + 1.3)
                            + 0.2 * Math.sin(seconds * 61.1 + 0.4));
                    glow(g2, x, y, radius * (1.15f + 0.45f * flick), light.color(), FLICKER_ALPHA + 0.3f * flick);
                }
                case GLINT -> {
                    double since = seconds % GLINT_SECONDS;
                    if (since < GLINT_SHOWS_SECONDS) {
                        float shine = (float) Math.sin(Math.PI * since / GLINT_SHOWS_SECONDS);
                        g2.setColor(faded(light.color(), shine));
                        g2.fill(glint(x, y, radius * (0.4f + 0.6f * shine)));
                    }
                }
                case SWEEP -> {
                    double turn = 2 * Math.PI * seconds / SWEEP_SECONDS;
                    g2.setPaint(new RadialGradientPaint(x, y, radius, new float[] {0f, 1f},
                            new Color[] {faded(light.color(), SWEEP_ALPHA), faded(light.color(), 0f)}));
                    g2.fill(ray(x, y, radius, turn));
                    g2.fill(ray(x, y, radius, turn + Math.PI));
                }
            }
        }
    }

    private static void glow(Graphics2D g2, float x, float y, float radius, Color color, float alpha) {
        if (radius <= 0f || alpha <= 0f) {
            return;
        }
        g2.setPaint(new RadialGradientPaint(x, y, radius, new float[] {0f, 1f},
                new Color[] {faded(color, alpha), faded(color, 0f)}));
        g2.fill(new Ellipse2D.Float(x - radius, y - radius, radius * 2, radius * 2));
    }

    private static Shape glint(float x, float y, float size) {
        float waist = size * 0.16f;
        Path2D.Float star = new Path2D.Float();
        star.moveTo(x, y - size);
        star.lineTo(x + waist, y - waist);
        star.lineTo(x + size, y);
        star.lineTo(x + waist, y + waist);
        star.lineTo(x, y + size);
        star.lineTo(x - waist, y + waist);
        star.lineTo(x - size, y);
        star.lineTo(x - waist, y - waist);
        star.closePath();
        return star;
    }

    private static Shape ray(float x, float y, float reach, double bearing) {
        Path2D.Float ray = new Path2D.Float();
        ray.moveTo(x, y);
        ray.lineTo(x + Math.cos(bearing - SWEEP_HALF_WIDTH) * reach, y + Math.sin(bearing - SWEEP_HALF_WIDTH) * reach);
        ray.lineTo(x + Math.cos(bearing + SWEEP_HALF_WIDTH) * reach, y + Math.sin(bearing + SWEEP_HALF_WIDTH) * reach);
        ray.closePath();
        return ray;
    }

    private static Color faded(Color color, float alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.round(Math.clamp(alpha, 0f, 1f) * 255));
    }

    private static Color mixed(Color from, Color to, float share) {
        return new Color(Math.round(from.getRed() + (to.getRed() - from.getRed()) * share),
                Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * share),
                Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * share));
    }

    // ---- pictures ------------------------------------------------------------------------------------------------

    private static Sprite sprite(TowerLook look, int tier, TowerPose pose, int facing, int scale, float device) {
        int shown = Math.clamp(tier, 0, TowerFoundations.TIERS - 1);
        Key key = new Key(new Posture(look, shown, pose), facing, Math.round(scale * device * DEVICE_STEPS_PER_UNIT));
        Sprite kept = SPRITES.get(key);
        if (kept != null) {
            return kept;
        }
        if (KEPT_PIXELS.get() > PIXEL_BUDGET) {
            SPRITES.clear();
            KEPT_PIXELS.set(0);
        }
        TowerFoundations.Foundation foundation = TowerFoundations.of(look, shown, pose);
        List<TowerFoundations.Part> parts;
        if (facing == BODY) {
            parts = foundation.parts();
        } else if (facing == FRONT) {
            parts = foundation.front();
        } else {
            AffineTransform turn = AffineTransform.getRotateInstance(facing * 2 * Math.PI / FACINGS);
            parts = foundation.head().stream().map(part -> part.withShape(turn.createTransformedShape(part.shape())))
                    .toList();
        }
        Sprite drawn = draw(parts, scale * device);
        if (SPRITES.putIfAbsent(key, drawn) == null) {
            KEPT_PIXELS.addAndGet((long) drawn.image().getWidth() * drawn.image().getHeight());
        }
        return drawn;
    }

    /**
     * Paints {@code parts} as they are, with a pen in cell units that draws {@code pixels} device pixels to a cell: for
     * a drawing too rare to keep a picture of.
     */
    static void paintParts(Graphics2D g, List<TowerFoundations.Part> parts, double pixels) {
        for (TowerFoundations.Part part : parts) {
            paintPart(g, part, pixels);
        }
    }

    /** Draws {@code parts} into a picture, {@code pixels} device pixels to a cell. */
    private static Sprite draw(List<TowerFoundations.Part> parts, double pixels) {
        Rectangle2D reach = new Rectangle2D.Double();
        boolean first = true;
        for (TowerFoundations.Part part : parts) {
            Rectangle2D bounds = part.shape().getBounds2D();
            double top = -LIFT * (part.z0() + part.height());
            double grow = part.width() / 2 + RIM;
            Rectangle2D lifted = new Rectangle2D.Double(bounds.getX() - grow, bounds.getY() + top - grow,
                    bounds.getWidth() + grow * 2, bounds.getHeight() - top - LIFT * part.z0() + grow * 2);
            if (first) {
                reach.setRect(lifted);
                first = false;
            } else {
                reach.add(lifted);
            }
        }
        int centreX = (int) Math.ceil(-reach.getMinX() * pixels) + PADDING;
        int centreY = (int) Math.ceil(-reach.getMinY() * pixels) + PADDING;
        int width = centreX + (int) Math.ceil(reach.getMaxX() * pixels) + PADDING;
        int height = centreY + (int) Math.ceil(reach.getMaxY() * pixels) + PADDING;
        BufferedImage image = new BufferedImage(Math.max(1, width), Math.max(1, height), BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.translate(centreX, centreY);
        g.scale(pixels, pixels);
        for (TowerFoundations.Part part : parts) {
            paintPart(g, part, pixels);
        }
        g.dispose();
        return new Sprite(image, centreX, centreY);
    }

    /**
     * One part: a solid one's side swept from its foot to its top a half pixel at a time, its top in the highlight,
     * then in its body tone shifted down and right inside the top, which leaves a lit rim along the top-left edge.
     */
    private static void paintPart(Graphics2D g, TowerFoundations.Part part, double pixels) {
        double foot = -LIFT * part.z0();
        double top = -LIFT * (part.z0() + part.height());
        switch (part.kind()) {
            case SOLID -> {
                int steps = Math.max(1, (int) Math.ceil(Math.abs(top - foot) * pixels / 0.5));
                g.setColor(part.tones().shade());
                for (int i = 0; i <= steps; i++) {
                    g.fill(moved(part.shape(), 0, foot + (top - foot) * i / steps));
                }
                Shape lid = moved(part.shape(), 0, top);
                g.setColor(part.tones().highlight());
                g.fill(lid);
                Shape clip = g.getClip();
                g.clip(lid);
                g.setColor(part.tones().body());
                g.fill(moved(lid, RIM, RIM));
                g.setClip(clip);
            }
            case FLAT -> {
                g.setColor(part.color());
                g.fill(moved(part.shape(), 0, foot));
            }
            case GLOW -> {
                Rectangle2D r = part.shape().getBounds2D();
                Color c = part.color();
                g.setPaint(new RadialGradientPaint((float) r.getCenterX(), (float) (r.getCenterY() + foot),
                        (float) (r.getWidth() / 2), new float[] {0f, 1f},
                        new Color[] {c, new Color(c.getRed(), c.getGreen(), c.getBlue(), 0)}));
                g.fill(moved(part.shape(), 0, foot));
            }
            case LINE -> {
                g.setColor(part.color());
                g.setStroke(new BasicStroke((float) part.width(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(moved(part.shape(), 0, foot));
            }
        }
    }

    private static Shape moved(Shape shape, double dx, double dy) {
        return AffineTransform.getTranslateInstance(dx, dy).createTransformedShape(shape);
    }
}
