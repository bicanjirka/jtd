package td.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A soft round light: its colour at full strength in the middle, fading to nothing at its edge. One picture per colour
 * is kept and placed at any size and strength, since filling a gradient again for every light every frame costs far
 * more than placing a picture. A glow sets its own strength, so it is never painted under a fading composite.
 */
final class Glows {

    /** The kept picture's radius in pixels: enough that a large glow, scaled up, still looks smooth. */
    private static final int KEPT_RADIUS = 48;
    private static final int MOST_KEPT = 256;

    private static final Map<Integer, BufferedImage> KEPT = new ConcurrentHashMap<>();

    private Glows() {
    }

    /** A glow of {@code color} {@code radius} board pixels round {@code x, y}, at {@code alpha} of its strength. */
    static void paint(Graphics2D g2, double x, double y, double radius, Color color, double alpha) {
        if (radius <= 0 || alpha <= 0) {
            return;
        }
        int rgb = color.getRGB() & 0xffffff;
        BufferedImage picture = KEPT.get(rgb);
        if (picture == null) {
            if (KEPT.size() >= MOST_KEPT) {
                KEPT.clear();
            }
            picture = draw(rgb);
            KEPT.put(rgb, picture);
        }
        Composite composite = g2.getComposite();
        float strength = (float) Math.min(1, alpha * color.getAlpha() / 255.0);
        AffineTransform board = g2.getTransform();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, strength));
        g2.translate(x - radius, y - radius);
        g2.scale(radius / KEPT_RADIUS, radius / KEPT_RADIUS);
        g2.drawImage(picture, 0, 0, null);
        g2.setTransform(board);
        g2.setComposite(composite);
    }

    private static BufferedImage draw(int rgb) {
        BufferedImage image = new BufferedImage(KEPT_RADIUS * 2, KEPT_RADIUS * 2, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color color = new Color(rgb);
        g.setPaint(new RadialGradientPaint(KEPT_RADIUS, KEPT_RADIUS, KEPT_RADIUS, new float[] {0f, 1f},
                new Color[] {color, new Color(rgb & 0xffffff, true)}));
        g.fill(new Ellipse2D.Double(0, 0, KEPT_RADIUS * 2, KEPT_RADIUS * 2));
        g.dispose();
        return image;
    }
}
