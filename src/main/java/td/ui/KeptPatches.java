package td.ui;

import td.ui.render.ZoneArcDraw;
import td.ui.render.ZoneDraw;
import td.ui.render.ZonePatchDraw;
import td.util.ThreadConfined;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Pictures of the ground patches that stay as they are from frame to frame (a field round a standing tower), each
 * drawn once into an image the size of the patch on screen and placed whole while it stays the same. A patch is kept
 * only while frames still draw it. What its drawing reads of a disc's clock is drawn as it was when kept, so only a
 * patch with nothing that moves may be kept.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
final class KeptPatches {

    private static final int PADDING = 3;
    private static final double TRANSLATE_STEPS = 16;

    private Map<Key, Kept> kept = new HashMap<>();
    private Map<Key, Kept> drawn = new HashMap<>();

    /** A patch without its discs' clock: a kept picture holds only what lies still. */
    private record Key(ZonePatchDraw still, double scale, long stepX, long stepY) {
    }

    private record Kept(BufferedImage image, int deviceX, int deviceY) {
    }

    /** Paints {@code patch} as {@code drawing} draws it, from its kept picture when it has one. */
    void paint(Graphics2D g2, ZonePatchDraw patch, double reach, BiConsumer<Graphics2D, ZonePatchDraw> drawing) {
        AffineTransform board = g2.getTransform();
        boolean plain = board.getShearX() == 0 && board.getShearY() == 0 && board.getScaleX() > 0
                && Math.abs(board.getScaleX() - board.getScaleY()) < 1e-6;
        if (!plain || patch.discs().isEmpty()) {
            drawing.accept(g2, patch);
            return;
        }
        double scale = board.getScaleX();
        ZonePatchDraw still = new ZonePatchDraw(patch.palette(),
                patch.discs().stream().map(zone -> new ZoneDraw(zone.centerX(), zone.centerY(), zone.radius(), zone.fade(), 0f))
                        .toList(), patch.edge());
        Key key = new Key(still, scale, Math.round(fraction(board.getTranslateX()) * TRANSLATE_STEPS),
                Math.round(fraction(board.getTranslateY()) * TRANSLATE_STEPS));
        Kept picture = this.drawn.get(key);
        if (picture == null) {
            picture = this.kept.get(key);
            if (picture == null) {
                picture = draw(board, patch, reach, drawing);
            }
            this.drawn.put(key, picture);
        }
        g2.setTransform(new AffineTransform());
        g2.drawImage(picture.image(), picture.deviceX(), picture.deviceY(), null);
        g2.setTransform(board);
    }

    /** Forgets the pictures this frame didn't draw. Call once a frame is painted. */
    void endFrame() {
        this.kept = this.drawn;
        this.drawn = new HashMap<>();
    }

    private static Kept draw(AffineTransform board, ZonePatchDraw patch, double reach,
            BiConsumer<Graphics2D, ZonePatchDraw> drawing) {
        Rectangle2D bounds = null;
        for (ZoneDraw zone : patch.discs()) {
            float r = zone.radius();
            Rectangle2D disc = new Rectangle2D.Float(zone.centerX() - r, zone.centerY() - r, r * 2, r * 2);
            bounds = bounds == null ? disc : bounds.createUnion(disc);
        }
        for (ZoneArcDraw arc : patch.edge()) {
            float r = arc.radius();
            bounds = bounds.createUnion(new Rectangle2D.Float(arc.centerX() - r, arc.centerY() - r, r * 2, r * 2));
        }
        double scale = board.getScaleX();
        int deviceX = (int) Math.floor(board.getTranslateX() + scale * (bounds.getMinX() - reach)) - PADDING;
        int deviceY = (int) Math.floor(board.getTranslateY() + scale * (bounds.getMinY() - reach)) - PADDING;
        int width = (int) Math.ceil(scale * (bounds.getWidth() + reach * 2)) + PADDING * 2 + 1;
        int height = (int) Math.ceil(scale * (bounds.getHeight() + reach * 2)) + PADDING * 2 + 1;
        BufferedImage image = new BufferedImage(Math.max(1, width), Math.max(1, height), BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.translate(board.getTranslateX() - deviceX, board.getTranslateY() - deviceY);
        g.scale(scale, scale);
        drawing.accept(g, patch);
        g.dispose();
        return new Kept(image, deviceX, deviceY);
    }

    private static double fraction(double value) {
        return value - Math.floor(value);
    }
}
