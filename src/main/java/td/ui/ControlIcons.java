package td.ui;

import javax.swing.Icon;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/**
 * Vector icons for the console's transport controls (play, pause, fast, super fast), drawn in
 * code like the rest of the game's art rather than loaded from image files.
 * <p>
 * These are deliberately not in {@link Java2DFrameRenderer}: that class owns board content, and
 * a transport control has no board counterpart to stay in sync with. What they do share is the
 * console's own foreground colour, so the buttons read as part of the same panel.
 */
final class ControlIcons {

    /** Matches the console's text colour, so the controls sit in the same visual family. */
    private static final Color FOREGROUND = new Color(220, 255, 220);
    private static final int SIZE = 14;

    private ControlIcons() {
    }

    static Icon play() {
        return render(g2 -> g2.fill(rightTriangle(0, SIZE * 0.62f)));
    }

    static Icon pause() {
        return render(g2 -> {
            float barWidth = SIZE * 0.22f;
            float height = SIZE * 0.72f;
            g2.fill(new Rectangle2D.Float(-SIZE * 0.28f, -height / 2, barWidth, height));
            g2.fill(new Rectangle2D.Float(SIZE * 0.06f, -height / 2, barWidth, height));
        });
    }

    static Icon fast() {
        return chevrons(2);
    }

    static Icon superFast() {
        return chevrons(3);
    }

    /** {@code count} right-pointing triangles in a row, centred as a group. */
    private static Icon chevrons(int count) {
        float size = SIZE * 0.5f;
        float step = size * 0.86f;
        float firstX = -step * (count - 1) / 2f;
        return render(g2 -> {
            for (int i = 0; i < count; i++) {
                g2.fill(rightTriangle(firstX + i * step, size));
            }
        });
    }

    /** A triangle pointing along +X, centred on {@code (centerX, 0)}. */
    private static Shape rightTriangle(float centerX, float size) {
        float half = size / 2;
        GeneralPath p = new GeneralPath();
        p.moveTo(centerX - half, -half);
        p.lineTo(centerX + half, 0);
        p.lineTo(centerX - half, half);
        p.closePath();
        return p;
    }

    /**
     * Rasterizes one icon with the origin at its centre, so each shape above can be written in
     * centre-relative coordinates the way the board's shapes are.
     */
    private static Icon render(Consumer<Graphics2D> paint) {
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.translate(SIZE / 2.0, SIZE / 2.0);
        g2.setColor(FOREGROUND);
        paint.accept(g2);
        g2.dispose();
        return new SharpImageIcon(image);
    }
}
