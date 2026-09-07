package td.ui;

import javax.swing.Icon;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * An {@link Icon} that paints a {@link BufferedImage} with bilinear interpolation.
 * {@code javax.swing.ImageIcon.paintIcon} draws straight onto the {@code Graphics} it is
 * given with no interpolation hint of its own, so under a scaled (HiDPI per-monitor)
 * {@code Graphics2D} transform it falls back to nearest-neighbor and looks blocky - the
 * same failure mode {@link Java2DFrameRenderer} sets {@code KEY_INTERPOLATION} to avoid
 * for the board itself.
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
