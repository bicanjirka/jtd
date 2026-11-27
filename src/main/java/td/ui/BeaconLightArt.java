package td.ui;

import td.ui.render.LightArc;
import td.ui.render.LightEdgeDraw;
import td.ui.render.LitEnemyDraw;
import td.ui.render.Palette;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.List;

/**
 * The Beacon's light, painted live in board pixels: its edge as it stirs, and an enemy standing in it, lit from the
 * top left as the board is. Each is a pure function of its draw, so nothing is kept between frames.
 */
final class BeaconLightArt {

    /** How the edge's moment is shaped: it grows for this share, holds to the second, then fades. */
    private static final float GROW_SHARE = 0.4f;
    private static final float FADE_IN_SHARE = 0.2f;
    private static final float FADE_OUT_FROM = 0.6f;
    /** The highlight that travels once round the rim finishes its turn by this share, and is this wide, in radians. */
    private static final float SWEEP_SHARE = 0.8f;
    private static final float SWEEP_WIDTH = 0.55f;
    /** The rim's soft layers: stroke width in cells, then opacity. */
    private static final float[] RIM_WIDTH_CELLS = {0.135f, 0.068f, 0.023f};
    private static final float[] RIM_ALPHA = {0.06f, 0.10f, 0.55f};
    private static final float POOL_CENTRE_ALPHA = 0.10f;
    private static final float POOL_EDGE_ALPHA = 0.05f;
    private static final int PRISM_SEGMENTS = 72;
    /** How wide a Lit tower's colour spreads round the rim, in radians. */
    private static final float PRISM_SPREAD = 0.62f;

    /** The direction shadows fall, down and to the right, so the light comes the other way. */
    private static final float[] DOWN = {0.57f, 0.82f};
    private static final float BEAM_LENGTH_CELLS = 1.56f;
    private static final float BEAM_ALPHA = 0.12f;
    private static final float POOL_ALPHA = 0.34f;
    private static final float RIM_LIGHT_ALPHA = 0.6f;
    private static final float BREATH_SECONDS = 2f;
    private static final int POOL_LAYERS = 3;
    private static final int MOTES = 2;
    private static final float MOTE_SECONDS = 2.2f;
    private static final float FRINGE_SECONDS = 0.5f;

    private BeaconLightArt() {
    }

    static void edge(Graphics2D g2, LightEdgeDraw edge) {
        float progress = edge.progress();
        float envelope = progress < FADE_IN_SHARE ? progress / FADE_IN_SHARE
                : progress < FADE_OUT_FROM ? 1f : 1f - (progress - FADE_OUT_FROM) / (1f - FADE_OUT_FROM);
        float grown = easeOut(progress / GROW_SHARE);
        float radius = edge.radius() * grown;
        if (radius < 1f || envelope <= 0f) {
            return;
        }
        Color warm = Java2DFrameRenderer.colorFor(Palette.TOWER_AURA_LIGHT);
        Color high = Java2DFrameRenderer.colorFor(Palette.TOWER_AURA_LIGHT_HIGH);
        float cell = edge.radius() / 2.5f;
        float cx = edge.centerX();
        float cy = edge.centerY();
        g2.setPaint(new RadialGradientPaint(new Point2D.Float(cx, cy), radius, new float[]{0f, 0.7f, 1f},
                new Color[]{alpha(high, POOL_CENTRE_ALPHA * envelope), alpha(warm, POOL_EDGE_ALPHA * 0.6f * envelope),
                        alpha(warm, POOL_EDGE_ALPHA * 1.8f * envelope)}));
        g2.fill(new Ellipse2D.Float(cx - radius, cy - radius, 2 * radius, 2 * radius));
        double sweep = -Math.PI / 2 + 2 * Math.PI * Math.min(1f, progress / SWEEP_SHARE);
        float sweeping = progress < SWEEP_SHARE ? 1f : 0.3f;
        if (edge.arcs().isEmpty()) {
            warmRim(g2, cx, cy, radius, cell, envelope, sweep, sweeping, warm, high);
        } else {
            prismRim(g2, edge, radius, cell, envelope, sweep, sweeping, warm);
        }
    }

    private static void warmRim(Graphics2D g2, float cx, float cy, float radius, float cell, float envelope,
            double sweep, float sweeping, Color warm, Color high) {
        for (int pass = 0; pass < RIM_WIDTH_CELLS.length; pass++) {
            g2.setStroke(new BasicStroke(RIM_WIDTH_CELLS[pass] * cell * 2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(alpha(warm, RIM_ALPHA[pass] * envelope * 0.55f));
            g2.draw(new Ellipse2D.Float(cx - radius, cy - radius, 2 * radius, 2 * radius));
            g2.setColor(alpha(high, RIM_ALPHA[pass] * envelope * sweeping));
            double start = Math.toDegrees(-sweep) - Math.toDegrees(SWEEP_WIDTH);
            g2.draw(new Arc2D.Double(cx - radius, cy - radius, 2 * radius, 2 * radius, start,
                    2 * Math.toDegrees(SWEEP_WIDTH), Arc2D.OPEN));
        }
    }

    private static void prismRim(Graphics2D g2, LightEdgeDraw edge, float radius, float cell, float envelope,
            double sweep, float sweeping, Color warm) {
        float cx = edge.centerX();
        float cy = edge.centerY();
        double step = 2 * Math.PI / PRISM_SEGMENTS;
        for (int pass = 0; pass < RIM_WIDTH_CELLS.length; pass++) {
            g2.setStroke(new BasicStroke(RIM_WIDTH_CELLS[pass] * cell * 2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < PRISM_SEGMENTS; i++) {
                double a0 = step * i - Math.PI;
                double near = Math.exp(-Math.pow(angularDistance(a0, sweep) / SWEEP_WIDTH, 2));
                g2.setColor(alpha(blendAt(a0, edge.arcs(), warm), RIM_ALPHA[pass] * envelope * (0.55f + 0.9f * (float) near * sweeping)));
                g2.draw(new Arc2D.Double(cx - radius, cy - radius, 2 * radius, 2 * radius, -Math.toDegrees(a0),
                        -Math.toDegrees(step) * 1.4, Arc2D.OPEN));
            }
        }
    }

    /** The rim's colour at {@code angle}: the warm light, leaning toward each Lit tower's mark the nearer it is. */
    private static Color blendAt(double angle, List<LightArc> arcs, Color warm) {
        double minShare = 0.35;
        double r = warm.getRed() * minShare;
        double g = warm.getGreen() * minShare;
        double b = warm.getBlue() * minShare;
        double weight = minShare;
        for (LightArc arc : arcs) {
            Color mark = Java2DFrameRenderer.colorFor(arc.palette());
            double k = Math.exp(-Math.pow(angularDistance(angle, arc.angleRadians()) / PRISM_SPREAD, 2));
            r += mark.getRed() * k;
            g += mark.getGreen() * k;
            b += mark.getBlue() * k;
            weight += k;
        }
        return new Color((int) (r / weight), (int) (g / weight), (int) (b / weight));
    }

    static void litEnemy(Graphics2D g2, LitEnemyDraw lit) {
        float cell = lit.cellPixels();
        Color warm = Java2DFrameRenderer.colorFor(Palette.TOWER_AURA_LIGHT);
        Color high = Java2DFrameRenderer.colorFor(Palette.TOWER_AURA_LIGHT_HIGH);
        float breath = 0.85f + 0.15f * (float) Math.sin(2 * Math.PI * lit.seconds() / BREATH_SECONDS + lit.phase());
        float k = lit.light() * breath;
        float ex = lit.x();
        float ey = lit.y();
        boolean detailed = !lit.crowded();
        float length = BEAM_LENGTH_CELLS * cell;
        float ox = ex - DOWN[0] * length;
        float oy = ey - DOWN[1] * length;
        float nx = -DOWN[1];
        float ny = DOWN[0];
        float footX = ex + 0.05f * cell;
        float footY = ey + 0.18f * cell;
        float top = 0.07f * cell;
        float bottom = 0.25f * cell;
        Path2D beam = new Path2D.Float();
        beam.moveTo(ox + nx * top, oy + ny * top);
        beam.lineTo(ox - nx * top, oy - ny * top);
        beam.lineTo(footX - nx * bottom, footY - ny * bottom);
        beam.lineTo(footX + nx * bottom, footY + ny * bottom);
        beam.closePath();
        if (detailed) {
            g2.setPaint(new GradientPaint(ox, oy, alpha(high, 0f), ex, ey, alpha(high, BEAM_ALPHA * k)));
            g2.fill(beam);
        }
        float poolX = 0.375f * cell;
        float poolY = 0.135f * cell;
        for (int layer = 0; layer < POOL_LAYERS; layer++) {
            float share = 1f - layer / (float) POOL_LAYERS;
            g2.setColor(alpha(layer == POOL_LAYERS - 1 ? high : warm, POOL_ALPHA * 0.45f * k));
            g2.fill(new Ellipse2D.Float(footX - poolX * share, footY - poolY * share, 2 * poolX * share, 2 * poolY * share));
        }
        float body = 0.22f * cell;
        g2.setStroke(new BasicStroke(0.06f * cell, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(alpha(warm, 0.12f * k));
        g2.draw(new Arc2D.Float(ex - body, ey - body, 2 * body, 2 * body, 98, 84, Arc2D.OPEN));
        g2.setStroke(new BasicStroke(0.02f * cell, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(alpha(high, RIM_LIGHT_ALPHA * k));
        g2.draw(new Arc2D.Float(ex - body, ey - body, 2 * body, 2 * body, 98, 84, Arc2D.OPEN));
        for (int m = 0; detailed && m < MOTES; m++) {
            float u = (lit.seconds() / MOTE_SECONDS + m / (float) MOTES + lit.phase() * 0.16f) % 1f;
            float sway = (float) Math.sin(u * 9 + m * 2 + lit.phase()) * 0.09f * cell;
            float px = ox + (ex - ox) * u + nx * sway;
            float py = oy + (ey - oy) * u + ny * sway;
            float a = (float) Math.sin(Math.PI * u) * 0.8f * k;
            g2.setColor(alpha(high, 0.9f * a));
            g2.fill(new Ellipse2D.Float(px - 0.02f * cell, py - 0.02f * cell, 0.04f * cell, 0.04f * cell));
        }
        if (detailed && !lit.fringe().isEmpty()) {
            fringe(g2, lit, k, ox, oy, footX, footY, nx, ny);
        }
    }

    /** Prism: the beam's lower edge and the pool's far side split into the marks the light carries. */
    private static void fringe(Graphics2D g2, LitEnemyDraw lit, float k, float ox, float oy, float footX, float footY,
            float nx, float ny) {
        float cell = lit.cellPixels();
        List<Palette> marks = lit.fringe();
        int shift = (int) Math.floor(lit.seconds() / FRINGE_SECONDS);
        g2.setStroke(new BasicStroke(0.018f * cell));
        for (int i = 0; i < marks.size(); i++) {
            Color mark = Java2DFrameRenderer.colorFor(marks.get((i + shift) % marks.size()));
            float off = (0.25f + i * 0.027f) * cell;
            float topOff = (0.07f + i * 0.007f) * cell;
            g2.setColor(alpha(mark, 0.22f * k));
            g2.draw(new Line2D.Float(ox + nx * topOff, oy + ny * topOff, footX + nx * off, footY + ny * off));
        }
        float poolX = 0.35f * cell;
        float poolY = 0.125f * cell;
        float span = 360f / (marks.size() * 3f);
        for (int i = 0; i < marks.size(); i++) {
            Color mark = Java2DFrameRenderer.colorFor(marks.get((i + shift) % marks.size()));
            float start = -62 + i * (span + 2f);
            g2.setStroke(new BasicStroke(0.028f * cell));
            g2.setColor(alpha(mark, 0.9f * k));
            g2.draw(new Arc2D.Float(footX - poolX, footY - poolY, 2 * poolX, 2 * poolY, start, span, Arc2D.OPEN));
        }
    }

    private static float easeOut(float x) {
        float c = Math.clamp(x, 0f, 1f);
        return 1f - (float) Math.pow(1f - c, 3);
    }

    private static double angularDistance(double a, double b) {
        double d = Math.abs(a - b) % (2 * Math.PI);
        return d > Math.PI ? 2 * Math.PI - d : d;
    }

    private static Color alpha(Color base, float opacity) {
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), Math.round(Math.clamp(opacity, 0f, 1f) * 255));
    }
}
