package td.ui;

import td.ui.render.EnemyBurstDraw;
import td.ui.render.EnemyEggDraw;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;

/**
 * Paints what an effect draws on an enemy. Every particle is a pure function of the clock and the enemy's seed, so
 * nothing is stored. Fixed sizes here are pixels on screen at the default window: each paint scales by
 * {@link #BOARD_PIXELS_PER_SCREEN_PIXEL} first and reads board sizes through it, so bodies and art keep their proportions.
 */
final class EnemyEffects {

    private static final float BOARD_PIXELS_PER_SCREEN_PIXEL = 0.82f;
    private static final double TAU = Math.PI * 2;

    private static final Color SHIELD_RIM = new Color(0xFF, 0xDC, 0x78);
    private static final Color SHIELD_FLASH = new Color(0xFF, 0xF4, 0xC8);
    private static final Color SHIELD_HIGHLIGHT = new Color(0xFF, 0xF8, 0xE0);
    private static final float EGG_BREATH = 0.012f;
    private static final int POP_ARCS = 9;
    private static final float POP_GROWTH = 0.7f;

    private EnemyEffects() {
    }

    /** A stable number from 0 to 1 for a seed, so a particle keeps its place and timing for its enemy's whole life. */
    private static double hash(double n) {
        double x = Math.sin(n * 127.1 + 11.7) * 43758.5453;
        return x - Math.floor(x);
    }

    private static Color faded(Color color, double alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) Math.round(Math.clamp(alpha, 0.0, 1.0) * 255));
    }

    private static float toScreen(float boardPixels) {
        return boardPixels / BOARD_PIXELS_PER_SCREEN_PIXEL;
    }

    private static void enter(Graphics2D g2, float x, float y) {
        g2.translate(x, y);
        g2.scale(BOARD_PIXELS_PER_SCREEN_PIXEL, BOARD_PIXELS_PER_SCREEN_PIXEL);
    }

    private static Path2D eggPath(float rx, float ry) {
        Path2D.Float egg = new Path2D.Float();
        egg.moveTo(0, -ry);
        egg.curveTo(rx * 0.72f, -ry, rx, -ry * 0.18f, rx, ry * 0.2f);
        egg.curveTo(rx, ry * 0.72f, rx * 0.58f, ry * 0.92f, 0, ry * 0.92f);
        egg.curveTo(-rx * 0.58f, ry * 0.92f, -rx, ry * 0.72f, -rx, ry * 0.2f);
        egg.curveTo(-rx, -ry * 0.18f, -rx * 0.72f, -ry, 0, -ry);
        egg.closePath();
        return egg;
    }

    static void paintEgg(Graphics2D g2, EnemyEggDraw egg) {
        AffineTransform save = g2.getTransform();
        Stroke stroke = g2.getStroke();
        enter(g2, egg.x(), egg.y());
        float breath = 1f + (float) Math.sin(egg.seconds() * 2.6 + egg.seed()) * EGG_BREATH;
        float rx = toScreen(egg.radiusX()) * breath;
        float ry = toScreen(egg.radiusY()) * (2f - breath);
        g2.setColor(faded(egg.flash() > 0f ? SHIELD_FLASH : SHIELD_RIM, (0.85 + egg.flash() * 0.15) * egg.alpha()));
        g2.setStroke(new BasicStroke(1f + egg.flash() * 0.6f));
        g2.draw(eggPath(rx, ry));
        g2.setColor(faded(SHIELD_HIGHLIGHT, 0.7 * egg.alpha()));
        g2.setStroke(new BasicStroke(1.2f));
        g2.translate(-rx * 0.36f, -ry * 0.5f);
        g2.rotate(-0.75);
        g2.draw(new Arc2D.Float(-rx * 0.24f, -ry * 0.15f, rx * 0.48f, ry * 0.3f, -171f, -162f, Arc2D.OPEN));
        g2.setStroke(stroke);
        g2.setTransform(save);
    }

    static void paintBurst(Graphics2D g2, EnemyBurstDraw burst) {
        AffineTransform save = g2.getTransform();
        Stroke stroke = g2.getStroke();
        enter(g2, burst.x(), burst.y());
        switch (burst.look()) {
            case SHIELD_POP -> paintShieldPop(g2, burst);
        }
        g2.setStroke(stroke);
        g2.setTransform(save);
    }

    /** The egg flying apart: pieces of its rim rushing outward and a pale flash where it was. */
    private static void paintShieldPop(Graphics2D g2, EnemyBurstDraw burst) {
        float p = burst.progress();
        float rx = toScreen(burst.radiusX());
        float ry = toScreen(burst.radiusY());
        float grow = 1f + p * POP_GROWTH;
        g2.setStroke(new BasicStroke(1.4f));
        g2.setColor(faded(SHIELD_RIM, (1f - p) * burst.alpha()));
        for (int i = 0; i < POP_ARCS; i++) {
            double from = TAU * i / POP_ARCS + 0.3;
            double to = from + TAU / POP_ARCS * 0.42 * (1 - p * 0.5);
            g2.draw(new Line2D.Double(Math.cos(from) * rx * grow, Math.sin(from) * ry * grow,
                    Math.cos(to) * rx * grow, Math.sin(to) * ry * grow));
        }
        float flash = 0.5f - p * 1.6f;
        if (flash > 0f) {
            g2.setColor(faded(SHIELD_FLASH, flash * burst.alpha()));
            g2.fill(eggPath(rx * (1f + p * 0.2f), ry * (1f + p * 0.2f)));
        }
    }
}
