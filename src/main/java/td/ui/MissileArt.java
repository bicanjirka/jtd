package td.ui;

import td.ui.render.LoadedMissile;
import td.ui.render.Palette;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.MultipleGradientPaint;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

/**
 * Paints an Aerie's missile, one drawing for the nest and for flight: a blue dart, a red diamond shard or a pale green
 * wisp. The shapes are authored for a missile one unit long, heading {@code +X}.
 */
final class MissileArt {

    /** How far the halo of a missile that waits on a Release reaches, as a share of its length. */
    private static final float HALO = 0.8f;
    private static final Color SHADE_EYE = new Color(0x1d1f26);

    private MissileArt() {
    }

    static Palette paletteOf(LoadedMissile kind) {
        return switch (kind) {
            case FROST -> Palette.PROJECTILE_MISSILE;
            case RUBY -> Palette.PROJECTILE_RUBY;
            case SHADE -> Palette.PROJECTILE_SHADE;
        };
    }

    /** A missile {@code length} pixels long at the pen's origin, heading along the pen's {@code +X}; with a halo when {@code bright}. */
    static void paint(Graphics2D g2, LoadedMissile kind, float length, boolean bright) {
        AffineTransform save = g2.getTransform();
        Paint paint = g2.getPaint();
        Color colour = Java2DFrameRenderer.colorFor(paletteOf(kind));
        g2.scale(length, length);
        if (bright) {
            g2.setPaint(new RadialGradientPaint(0f, 0f, HALO, new float[] {0f, 1f},
                    new Color[] {faded(colour, 0.8), faded(colour, 0)}, MultipleGradientPaint.CycleMethod.NO_CYCLE));
            g2.fill(new Ellipse2D.Float(-HALO, -HALO, HALO * 2, HALO * 2));
        }
        switch (kind) {
            case FROST -> {
                g2.setColor(mix(colour, Color.BLACK, 0.3f));
                g2.fill(poly(0.6f, 0f, -0.4f, 0.33f, -0.2f, 0f, -0.4f, -0.33f));
                g2.setColor(mix(colour, Color.WHITE, 0.5f));
                g2.fill(poly(0.6f, 0f, -0.2f, 0f, -0.4f, -0.33f));
            }
            case RUBY -> {
                g2.setColor(mix(colour, Color.BLACK, 0.35f));
                g2.fill(poly(0.6f, 0f, 0f, 0.3f, -0.45f, 0f, 0f, -0.3f));
                g2.setColor(mix(colour, Color.WHITE, 0.35f));
                g2.fill(poly(0.6f, 0f, 0f, -0.3f, -0.45f, 0f));
            }
            case SHADE -> {
                g2.setColor(faded(colour, 0.35));
                g2.fill(poly(0f, -0.27f, -1f, 0f, 0f, 0.27f));
                g2.setColor(faded(colour, 0.9));
                g2.fill(new Ellipse2D.Float(-0.2f, -0.29f, 0.58f, 0.58f));
                g2.setColor(SHADE_EYE);
                g2.fill(new Ellipse2D.Float(0.05f, -0.12f, 0.24f, 0.24f));
            }
        }
        g2.setPaint(paint);
        g2.setTransform(save);
    }

    private static Path2D.Float poly(float... points) {
        Path2D.Float path = new Path2D.Float();
        path.moveTo(points[0], points[1]);
        for (int i = 2; i < points.length; i += 2) {
            path.lineTo(points[i], points[i + 1]);
        }
        path.closePath();
        return path;
    }

    private static Color mix(Color from, Color to, float share) {
        return new Color(Math.round(from.getRed() + (to.getRed() - from.getRed()) * share),
                Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * share),
                Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * share));
    }

    private static Color faded(Color colour, double alpha) {
        return new Color(colour.getRed(), colour.getGreen(), colour.getBlue(),
                (int) Math.round(Math.clamp(alpha, 0, 1) * 255));
    }
}
