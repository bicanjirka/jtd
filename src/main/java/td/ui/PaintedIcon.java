package td.ui;

import javax.swing.Icon;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.function.Consumer;

/**
 * An icon painted with vector calls around its centre, so it stays sharp at any HiDPI scale, unlike
 * a pre-rendered image.
 */
record PaintedIcon(int width, int height, Consumer<Graphics2D> painter) implements Icon {

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.translate(x + this.width / 2.0, y + this.height / 2.0);
        this.painter.accept(g2);
        g2.dispose();
    }

    @Override
    public int getIconWidth() {
        return this.width;
    }

    @Override
    public int getIconHeight() {
        return this.height;
    }
}
