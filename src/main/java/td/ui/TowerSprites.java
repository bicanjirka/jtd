package td.ui;

import td.ui.render.TowerLook;
import td.ui.render.TowerSpriteDraw;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Paints a tower in the raised view: a foundation of lit parts, the head it aims, and a soft shadow on the ground. A
 * foundation is tens of parts, so each is drawn once into a picture per tier and zoom and kept; a head is kept per
 * heading step. Placing a picture is all a frame costs.
 */
final class TowerSprites {

    /** How far up the screen a part's top is lifted, as a share of its height: the board is seen from the south. */
    private static final double LIFT = 0.42;
    /** The lit rim along a part's top-left edge, in cells. */
    private static final double RIM = 0.024;
    /** A head turns in this many steps; between two of them it reuses the nearer picture. */
    private static final int FACINGS = 64;
    private static final int FOUNDATION = -1;
    private static final float DEVICE_STEPS_PER_UNIT = 16f;
    /** Past this many kept pixels every picture is dropped and drawn again as it is needed. */
    private static final long PIXEL_BUDGET = 8_000_000L;
    private static final int PADDING = 2;
    private static final Color SHADOW = new Color(0, 0, 0, 150);
    /** How far the new foundation has spread when the rise ends, and how far above the cell's centre it spreads from. */
    private static final float RISE_SPREAD_CELLS = 1.2f;
    private static final float RISE_SPREAD_LIFT_CELLS = 0.3f;
    /** The rise's ring starts inside the foundation and ends past the cell's edge. */
    private static final float RISE_RING_FROM_CELLS = 0.2f;
    private static final float RISE_RING_TO_CELLS = 0.8f;
    private static final float RISE_RING_STROKE = 2f;

    private static final Map<Key, Sprite> SPRITES = new ConcurrentHashMap<>();
    private static final AtomicLong KEPT_PIXELS = new AtomicLong();

    private TowerSprites() {
    }

    /** {@code pixelSteps} is the device pixels to a cell, in sixteenths, so a zoom or a HiDPI scale gets its own. */
    private record Key(TowerLook look, int tier, int facing, int pixelSteps) {
    }

    /** A picture and where the tower's centre sits in it, in device pixels. */
    private record Sprite(BufferedImage image, int centreX, int centreY) {
    }

    /** The soft shadow a foundation casts on the ground, down and to the right. */
    static void paintShadow(Graphics2D g2, TowerSpriteDraw tower, int scale) {
        double footprint = TowerFoundations.of(tower.look(), tower.tier()).footprint() * scale;
        g2.setColor(SHADOW);
        g2.fill(new Ellipse2D.Double(tower.centerX() + footprint * 0.14 - footprint * 1.06,
                tower.centerY() + footprint * 0.16 - footprint * 1.02, footprint * 2.12, footprint * 2.04));
    }

    /**
     * The foundation and, for a look that aims, its head. While its tier rises, the new foundation spreads out from
     * the centre over the old one, and one ring in the new tier's colour runs out past the cell's edge and fades.
     */
    static void paint(Graphics2D g2, TowerSpriteDraw tower, int scale) {
        float progress = tower.riseProgress();
        if (progress < 0f) {
            paint(g2, tower.look(), tower.tier(), tower.centerX(), tower.centerY(), scale, tower.headingRadians());
            return;
        }
        paint(g2, tower.look(), tower.risenFrom(), tower.centerX(), tower.centerY(), scale, tower.headingRadians());
        Shape clip = g2.getClip();
        float spread = scale * RISE_SPREAD_CELLS * progress;
        float spreadY = tower.centerY() - scale * RISE_SPREAD_LIFT_CELLS;
        g2.clip(new Ellipse2D.Float(tower.centerX() - spread, spreadY - spread, spread * 2, spread * 2));
        paint(g2, tower.look(), tower.tier(), tower.centerX(), tower.centerY(), scale, tower.headingRadians());
        g2.setClip(clip);
        float radius = scale * (RISE_RING_FROM_CELLS + (RISE_RING_TO_CELLS - RISE_RING_FROM_CELLS) * progress);
        Color tier = TowerLooks.tierColor(tower.look(), tower.tier());
        Stroke stroke = g2.getStroke();
        g2.setStroke(new BasicStroke(RISE_RING_STROKE));
        g2.setColor(new Color(tier.getRed(), tier.getGreen(), tier.getBlue(), Math.round(230 * (1f - progress))));
        g2.draw(new Ellipse2D.Float(tower.centerX() - radius, tower.centerY() - radius, radius * 2, radius * 2));
        g2.setStroke(stroke);
    }

    /** As {@link #paint(Graphics2D, TowerSpriteDraw, int)}, for a look at a tier on its own. */
    static void paint(Graphics2D g2, TowerLook look, int tier, float centerX, float centerY, int scale, double heading) {
        AffineTransform board = g2.getTransform();
        boolean plain = board.getShearX() == 0 && board.getShearY() == 0 && board.getScaleX() > 0
                && Math.abs(board.getScaleX() - board.getScaleY()) < 1e-6;
        float device = plain ? Math.round((float) board.getScaleX() * DEVICE_STEPS_PER_UNIT) / DEVICE_STEPS_PER_UNIT : 1f;
        device = Math.max(device, 1f / DEVICE_STEPS_PER_UNIT);
        place(g2, board, plain, sprite(look, tier, FOUNDATION, scale, device), centerX, centerY);
        if (TowerFoundations.head(look).isPresent()) {
            int facing = Math.floorMod((int) Math.round(heading / (2 * Math.PI) * FACINGS), FACINGS);
            place(g2, board, plain, sprite(look, tier, facing, scale, device), centerX, centerY);
        }
    }

    private static void place(Graphics2D g2, AffineTransform board, boolean plain, Sprite sprite, float x, float y) {
        if (plain) {
            // Device pixel for device pixel, so the picture stays as sharp as the shapes it was drawn from.
            long deviceX = Math.round(board.getTranslateX() + board.getScaleX() * x);
            long deviceY = Math.round(board.getTranslateY() + board.getScaleY() * y);
            g2.setTransform(new AffineTransform());
            g2.drawImage(sprite.image(), (int) deviceX - sprite.centreX(), (int) deviceY - sprite.centreY(), null);
            g2.setTransform(board);
        } else {
            g2.drawImage(sprite.image(), Math.round(x) - sprite.centreX(), Math.round(y) - sprite.centreY(), null);
        }
    }

    private static Sprite sprite(TowerLook look, int tier, int facing, int scale, float device) {
        int shown = Math.clamp(tier, 0, TowerFoundations.TIERS - 1);
        Key key = new Key(look, shown, facing, Math.round(scale * device * DEVICE_STEPS_PER_UNIT));
        Sprite kept = SPRITES.get(key);
        if (kept != null) {
            return kept;
        }
        if (KEPT_PIXELS.get() > PIXEL_BUDGET) {
            SPRITES.clear();
            KEPT_PIXELS.set(0);
        }
        TowerFoundations.Foundation foundation = TowerFoundations.of(look, shown);
        List<TowerFoundations.Part> parts;
        if (facing == FOUNDATION) {
            parts = foundation.parts();
        } else {
            AffineTransform turn = AffineTransform.getRotateInstance(facing * 2 * Math.PI / FACINGS);
            parts = TowerFoundations.head(look).orElseThrow().stream()
                    .map(part -> part.withShape(turn.createTransformedShape(part.shape())).raisedBy(foundation.top()))
                    .toList();
        }
        Sprite drawn = draw(parts, scale * device);
        if (SPRITES.putIfAbsent(key, drawn) == null) {
            KEPT_PIXELS.addAndGet((long) drawn.image().getWidth() * drawn.image().getHeight());
        }
        return drawn;
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
