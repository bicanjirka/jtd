package td.ui;

import td.ui.render.Palette;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * The bare {@code $} that marks what sells back in full: the credits icon's own strokes, in its grey with a thin dark
 * outline so it reads on any ground, the same on every OS.
 */
final class CashMark {

    private static final Color INK = Java2DFrameRenderer.colorFor(Palette.TERM_STATUS_CASH);
    private static final Color OUTLINE = new Color(18, 18, 20);
    private static final float STROKE_UNITS = 1.75f;
    private static final float OUTLINE_UNITS = 1.4f;
    private static final Shape DOLLAR = dollar();
    private static final Rectangle2D BOUNDS = DOLLAR.getBounds2D();

    private CashMark() {
    }

    /** The credits coin's own dollar strokes. */
    private static Shape dollar() {
        Path2D.Float strokes = new Path2D.Float();
        PictogramCatalogue.of("mark.cash").orElseThrow().layers()
                .forEach(layer -> strokes.append(layer.shape(), false));
        return strokes;
    }

    /** The mark {@code height} pixels tall, centred on ({@code x}, {@code y}). */
    static void paint(Graphics2D g, float x, float y, float height) {
        Graphics2D mark = (Graphics2D) g.create();
        try {
            double k = height / (BOUNDS.getHeight() + STROKE_UNITS);
            mark.translate(x, y);
            mark.scale(k, k);
            mark.translate(-BOUNDS.getCenterX(), -BOUNDS.getCenterY());
            mark.setColor(OUTLINE);
            mark.setStroke(new BasicStroke(STROKE_UNITS + OUTLINE_UNITS, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
            mark.draw(DOLLAR);
            mark.setColor(INK);
            mark.setStroke(new BasicStroke(STROKE_UNITS, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            mark.draw(DOLLAR);
        } finally {
            mark.dispose();
        }
    }
}
