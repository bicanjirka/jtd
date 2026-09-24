package td.ui;

import javax.swing.Icon;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * Paints an image with bilinear interpolation, which {@code ImageIcon} doesn't set; without it a
 * HiDPI scale renders blocky.
 */
record SharpImageIcon(BufferedImage image) implements Icon {

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g;
        Object previous = g2.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(this.image, x, y, null);
        if (previous != null) {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, previous);
        }
    }

    @Override
    public int getIconWidth() {
        return this.image.getWidth();
    }

    @Override
    public int getIconHeight() {
        return this.image.getHeight();
    }
}
