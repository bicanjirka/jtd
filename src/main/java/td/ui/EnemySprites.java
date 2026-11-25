package td.ui;

import td.ui.render.EnemyBodyDraw;
import td.ui.render.EnemyLook;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Paints an enemy body as a small stack of lit parts, as a tower is built: a side that falls down the screen, a top with
 * a lit rim and a shaded far edge, a cap or a raised detail on it, a cast shadow and a glint. A body is many fills, so
 * each is drawn once into a picture and kept; only what moves with every step is painted live.
 */
final class EnemySprites {

    /** A body turns in this many steps; between two of them it reuses the nearer picture. */
    private static final int FACINGS = 64;
    private static final float SCALE_STEPS_PER_PIXEL = 4f;
    private static final float DEVICE_STEPS_PER_UNIT = 16f;
    /** Past this many kept pixels every picture is dropped and drawn again as it is needed. */
    private static final long PIXEL_BUDGET = 6_000_000L;
    private static final int PADDING = 2;

    private static final float RIM_SHARE = 0.09f;
    private static final float FAR_EDGE_SHARE = 0.14f;
    private static final float FAR_EDGE_DARKENING = 0.24f;
    private static final float CAP_LIGHTENING = 0.14f;
    private static final float SHADOW_SIDEWAYS = 0.16f;
    private static final float SHADOW_DOWN = 0.1f;
    private static final int CAST_SHADOW_ALPHA = 105;
    private static final Color GLINT = new Color(255, 255, 255, 135);
    private static final Color HOVER_SHADOW = new Color(104, 112, 136);
    private static final float HOVER_SHADOW_ALPHA = 0.36f;

    /** A walker lifts off the ground once a stride; how high, as a share of its scale. */
    private static final float HOP_SHARE = 0.1f;
    private static final float STRIDE_SCALES = 2.4f;
    private static final float HOVER_SWAY_SHARE = 0.07f;
    private static final float GLOW_ALPHA = 0.3f;

    private static final Map<Key, Sprite> SPRITES = new ConcurrentHashMap<>();
    private static final AtomicLong KEPT_PIXELS = new AtomicLong();

    private EnemySprites() {
    }

    /** A picture's size: the body's size and the zoom, each in steps. */
    private record Size(int scaleSteps, int deviceSteps) {
    }

    private record Key(EnemyLook look, Size size, int facing, SceneLight light) {
    }

    /** A body's picture and where its centre sits in it, in device pixels. */
    private record Sprite(BufferedImage image, int centreX, int centreY) {
    }

    /**
     * How far above its place on the ground a body is drawn right now, in board pixels: a flyer's hover, a walker's
     * hop. It follows the place itself, so a body that stands still holds still and the game's pause holds it too.
     */
    static float lift(EnemyLook look, float x, float y, float scale) {
        EnemyLooks.Form form = EnemyLooks.of(look).form();
        double stride = stride(x, y, scale);
        if (form.hover() > 0f) {
            return scale * (form.hover() + HOVER_SWAY_SHARE * (float) Math.sin(stride * 0.5));
        }
        return scale * HOP_SHARE * (float) Math.abs(Math.sin(stride));
    }

    private static double stride(float x, float y, float scale) {
        return (x * 0.71 + y * 1.13) / (scale * STRIDE_SCALES) * Math.PI;
    }

    static void paint(Graphics2D g2, EnemyBodyDraw body, SceneLight light) {
        EnemyLooks.Row row = EnemyLooks.of(body.look());
        EnemyLooks.Form form = row.form();
        float visibility = 1f - body.cloakProgress();
        float lift = lift(body.look(), body.x(), body.y(), body.scale());
        if (form.hover() > 0f) {
            paintHoverShadow(g2, body, row, visibility);
        }
        AffineTransform board = g2.getTransform();
        boolean plain = board.getShearX() == 0 && board.getShearY() == 0 && board.getScaleX() > 0
                && Math.abs(board.getScaleX() - board.getScaleY()) < 1e-6;
        float device = plain ? Math.round((float) board.getScaleX() * DEVICE_STEPS_PER_UNIT) / DEVICE_STEPS_PER_UNIT : 1f;
        Sprite sprite = sprite(body, row, Math.max(device, 1f / DEVICE_STEPS_PER_UNIT), light);
        Composite composite = g2.getComposite();
        if (visibility < 1f) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.clamp(visibility, 0f, 1f)));
        }
        if (plain) {
            // Device pixel for device pixel, so the picture stays as sharp as the shapes it was drawn from.
            long deviceX = Math.round(board.getTranslateX() + board.getScaleX() * body.x());
            long deviceY = Math.round(board.getTranslateY() + board.getScaleY() * (body.y() - lift));
            g2.setTransform(new AffineTransform());
            g2.drawImage(sprite.image(), (int) deviceX - sprite.centreX(), (int) deviceY - sprite.centreY(), null);
            g2.setTransform(board);
        } else {
            g2.drawImage(sprite.image(), Math.round(body.x()) - sprite.centreX(), Math.round(body.y() - lift) - sprite.centreY(), null);
        }
        g2.setComposite(composite);
        if (form.glowing()) {
            paintGlow(g2, body, row, lift, visibility);
        }
    }

    /** A flyer's shadow lies on the ground under it. The floors are near-black, so it is a pale veil, not a dark one. */
    private static void paintHoverShadow(Graphics2D g2, EnemyBodyDraw body, EnemyLooks.Row row, float visibility) {
        float halfWidth = body.scale() * row.reach() * 0.62f;
        float halfHeight = halfWidth * 0.42f;
        g2.setColor(faded(HOVER_SHADOW, HOVER_SHADOW_ALPHA * visibility));
        g2.fill(new Ellipse2D.Float(body.x() - halfWidth, body.y() + body.scale() * 0.2f - halfHeight, halfWidth * 2, halfHeight * 2));
    }

    /** A hot core breathes as its body moves. */
    private static void paintGlow(Graphics2D g2, EnemyBodyDraw body, EnemyLooks.Row row, float lift, float visibility) {
        Color color = row.detail().map(EnemyLooks.Detail::color).orElse(row.tones().highlight());
        double breath = Math.sin(stride(body.x(), body.y(), body.scale()) * 0.7);
        float radius = body.scale() * (0.55f + 0.12f * (float) breath);
        float centreY = body.y() - lift - body.scale() * (row.form().capHeight() + row.form().detailHeight());
        g2.setColor(faded(color, GLOW_ALPHA * (0.75f + 0.25f * (float) breath) * visibility));
        g2.fill(new Ellipse2D.Float(body.x() - radius, centreY - radius, radius * 2, radius * 2));
    }

    private static Color faded(Color color, float alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.round(Math.clamp(alpha, 0f, 1f) * 255f));
    }

    private static Sprite sprite(EnemyBodyDraw body, EnemyLooks.Row row, float device, SceneLight light) {
        int scaleSteps = Math.max(1, Math.round(body.scale() * SCALE_STEPS_PER_PIXEL));
        int facing = Math.floorMod(Math.round((float) (body.facingRadians() / (2 * Math.PI) * FACINGS)), FACINGS);
        Key key = new Key(body.look(), new Size(scaleSteps, Math.round(device * DEVICE_STEPS_PER_UNIT)), facing, light);
        Sprite kept = SPRITES.get(key);
        if (kept != null) {
            return kept;
        }
        if (KEPT_PIXELS.get() > PIXEL_BUDGET) {
            SPRITES.clear();
            KEPT_PIXELS.set(0);
        }
        Sprite drawn = draw(row, scaleSteps / SCALE_STEPS_PER_PIXEL, facing * 2 * Math.PI / FACINGS, device, light);
        if (SPRITES.putIfAbsent(key, drawn) == null) {
            KEPT_PIXELS.addAndGet((long) drawn.image().getWidth() * drawn.image().getHeight());
        }
        return drawn;
    }

    private static Sprite draw(EnemyLooks.Row row, float scale, double facing, float device, SceneLight light) {
        EnemyLooks.Form form = row.form();
        AffineTransform turn = AffineTransform.getRotateInstance(facing);
        Shape top = turn.createTransformedShape(row.body().at(scale));
        float height = form.height() * scale;
        float capLift = form.capShare() > 0f ? form.capHeight() * scale : 0f;
        float detailLift = capLift + form.detailHeight() * scale;
        float rim = Math.max(1f / device, scale * RIM_SHARE);
        float farEdge = Math.max(1f / device, scale * FAR_EDGE_SHARE);
        boolean grounded = form.hover() == 0f;

        Rectangle2D reach = top.getBounds2D();
        if (row.detail().isPresent()) {
            reach = reach.createUnion(turn.createTransformedShape(row.detail().get().shape().at(scale)).getBounds2D());
        }
        double left = reach.getMinX() - rim;
        double right = reach.getMaxX() + (grounded ? scale * SHADOW_SIDEWAYS : 0);
        double up = reach.getMinY() - detailLift - rim;
        double down = reach.getMaxY() + height + (grounded ? scale * SHADOW_DOWN : 0);
        int centreX = (int) Math.ceil(-left * device) + PADDING;
        int centreY = (int) Math.ceil(-up * device) + PADDING;
        int width = centreX + (int) Math.ceil(right * device) + PADDING;
        int imageHeight = centreY + (int) Math.ceil(down * device) + PADDING;
        BufferedImage image = new BufferedImage(width, imageHeight, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.translate(centreX, centreY);
        g.scale(device, device);

        if (grounded) {
            g.setColor(light.shadowOf(CAST_SHADOW_ALPHA));
            g.fill(moved(top, scale * SHADOW_SIDEWAYS, height + scale * SHADOW_DOWN));
        }
        Tones tones = light.on(row.tones());
        side(g, top, tones.shade(), height, 0f, device);
        face(g, top, tones, 0f, rim, farEdge);
        if (capLift > 0f) {
            Shape cap = turn.createTransformedShape(row.body().at(scale * form.capShare()));
            Tones capTones = light.on(Tones.of(toward(tones.body(), Color.WHITE, CAP_LIGHTENING)));
            side(g, cap, toward(tones.shade(), tones.body(), 0.35f), 0f, -capLift, device);
            face(g, cap, capTones, -capLift, rim, farEdge * form.capShare());
        }
        if (row.detail().isPresent()) {
            EnemyLooks.Detail detail = row.detail().get();
            Shape shape = turn.createTransformedShape(detail.shape().at(scale));
            Tones detailTones = light.on(Tones.of(detail.color()));
            if (form.detailHeight() > 0f) {
                side(g, shape, detailTones.shade(), -capLift, -detailLift, device);
                face(g, shape, detailTones, -detailLift, rim * 0.7f, 0f);
            } else {
                g.setColor(detail.color());
                g.fill(moved(shape, 0, -capLift));
            }
        }
        if (form.glossy()) {
            float spread = form.capShare() > 0f ? form.capShare() : 1f;
            Shape glint = AffineTransform.getRotateInstance(-0.7).createTransformedShape(
                    new Ellipse2D.Float(-scale * 0.2f * spread, -scale * 0.08f * spread, scale * 0.4f * spread, scale * 0.16f * spread));
            g.setColor(GLINT);
            g.fill(moved(glint, -scale * 0.3f * spread, -scale * 0.36f * spread - capLift));
        }
        g.dispose();
        return new Sprite(image, centreX, centreY);
    }

    /** A part's side: its outline swept between two heights, a device pixel at a time. */
    private static void side(Graphics2D g, Shape shape, Color color, float from, float to, float device) {
        int steps = Math.max(1, (int) Math.ceil(Math.abs(to - from) * device));
        g.setColor(color);
        for (int step = 0; step <= steps; step++) {
            g.fill(moved(shape, 0, from + (to - from) * step / steps));
        }
    }

    /** A part's top at {@code lift}: lit along its top-left rim, shaded along its far edge, its own colour between. */
    private static void face(Graphics2D g, Shape shape, Tones tones, float lift, float rim, float farEdge) {
        Shape top = moved(shape, 0, lift);
        Shape clip = g.getClip();
        g.setColor(tones.highlight());
        g.fill(top);
        g.clip(top);
        if (farEdge > 0f) {
            g.setColor(toward(tones.body(), tones.shade(), FAR_EDGE_DARKENING));
            g.fill(moved(top, rim, rim));
            g.clip(moved(top, -farEdge, -farEdge));
        }
        g.setColor(tones.body());
        g.fill(moved(top, rim, rim));
        g.setClip(clip);
    }

    private static Shape moved(Shape shape, double dx, double dy) {
        return AffineTransform.getTranslateInstance(dx, dy).createTransformedShape(shape);
    }

    private static Color toward(Color from, Color to, float share) {
        return new Color(Math.round(from.getRed() + (to.getRed() - from.getRed()) * share),
                Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * share),
                Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * share));
    }
}
