package td.ui;

import td.ui.render.Palette;
import td.ui.render.ZoneArcDraw;
import td.ui.render.ZoneDraw;
import td.ui.render.ZonePatchDraw;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The ground a Mortar's payload leaves: burning jelly, a pool of tar, a sheet of frost. Each lies flat as one even fill
 * under one outline, however many shells laid it; what stands on it (flames, bubbles, shards of ice) is lit from the top
 * left like everything else on the board, and what rises from it (smoke, mist) drifts on the wall clock. Every piece is
 * a function of its disc's place and the clock; nothing is stored.
 */
final class GroundArt {

    private static final Stroke EDGE = new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

    private static final Color BURNING_FILL = new Color(0x4a1608);
    private static final Color BURNING_EDGE = new Color(0xff7a2a);
    private static final Color SMOKE = new Color(0x3a3030);
    private static final Color TAR_FILL = new Color(0x1a1322);
    private static final Color TAR_EDGE = new Color(0x8a76aa);
    private static final Color FROST_FILL = new Color(0x9fd4f2);
    private static final Color FROST_EDGE = new Color(0xe6f8ff);
    private static final Color RIME = new Color(0xf0faff);

    private GroundArt() {
    }

    /** Whether this draws the ground of {@code look}. */
    static boolean draws(Palette look) {
        return look == Palette.ZONE_BURNING || look == Palette.ZONE_TAR || look == Palette.ZONE_FROST;
    }

    /** One patch of payload ground, {@code unit} pixels to a cell. */
    static void paint(Graphics2D g2, ZonePatchDraw patch, int unit) {
        Path2D ground = new Path2D.Float(Path2D.WIND_NON_ZERO);
        for (ZoneDraw zone : patch.discs()) {
            ground.append(BlastArt.disc(zone.centerX(), zone.centerY(), zone.radius()), false);
        }
        Path2D edge = new Path2D.Float(Path2D.WIND_NON_ZERO);
        for (ZoneArcDraw arc : patch.edge()) {
            float r = arc.radius();
            edge.append(new Arc2D.Float(arc.centerX() - r, arc.centerY() - r, r * 2, r * 2,
                    (float) -Math.toDegrees(arc.startRadians()), (float) -Math.toDegrees(arc.sweepRadians()),
                    Arc2D.OPEN), false);
        }
        Stroke stroke = g2.getStroke();
        if (patch.palette() == Palette.ZONE_BURNING) {
            burning(g2, patch, ground, edge, unit);
        } else if (patch.palette() == Palette.ZONE_TAR) {
            tar(g2, patch, ground, edge, unit);
        } else {
            frost(g2, patch, ground, edge, unit);
        }
        g2.setStroke(stroke);
    }

    private static void lie(Graphics2D g2, Path2D ground, Path2D edge, Color fill, double fillAlpha, Color rim,
                            double rimAlpha) {
        g2.setColor(BlastArt.faded(fill, fillAlpha));
        g2.fill(ground);
        g2.setStroke(EDGE);
        g2.setColor(BlastArt.faded(rim, rimAlpha));
        g2.draw(edge);
    }

    private static int seedOf(ZoneDraw zone) {
        return Math.round(zone.centerX() * 7 + zone.centerY() * 13);
    }

    /** A point of disc {@code zone} spread evenly over it by {@code i} of {@code n}, within {@code reach} of its radius. */
    private static double[] spot(ZoneDraw zone, int seed, int i, int n, double reach) {
        double angle = i * 2.4 + BlastArt.hash(seed, 300 + i);
        double out = zone.radius() * reach * Math.sqrt((i + 0.5) / n);
        return new double[] {zone.centerX() + Math.cos(angle) * out, zone.centerY() + Math.sin(angle) * out};
    }

    // ---- burning ground --------------------------------------------------------------------------------------------

    private static final int EMBERS = 12;
    private static final int WISPS = 2;

    /** Charred ground glowing with embers, flames standing all over it, and smoke curling up off it. */
    private static void burning(Graphics2D g2, ZonePatchDraw patch, Path2D ground, Path2D edge, int unit) {
        lie(g2, ground, edge, BURNING_FILL, 0.7, BURNING_EDGE, 0.55);
        List<double[]> flames = new ArrayList<>();
        for (ZoneDraw zone : patch.discs()) {
            int seed = seedOf(zone);
            double fade = zone.fade();
            TowerLifeArt.glow(g2, zone.centerX(), zone.centerY(), zone.radius() * 0.9, BlastArt.ORANGE_FIRE,
                    0.4 * fade);
            for (int i = 0; i < EMBERS; i++) {
                double[] at = spot(zone, seed, i, EMBERS, 0.9);
                double glow = 0.5 + 0.5 * Math.sin(zone.phase() * (3 + BlastArt.hash(seed, i) * 4) + i);
                BlastArt.flat(g2, at[0], at[1], unit * 0.022, BlastArt.mix(BlastArt.RED_FIRE, BlastArt.YELLOW_FIRE,
                        glow), (0.4 + 0.6 * glow) * fade);
            }
            int count = Math.max(3, Math.round(5 * zone.radius() * zone.radius() / (unit * unit)));
            for (int i = 0; i < count; i++) {
                double[] at = spot(zone, seed + 1, i, count, 0.72);
                flames.add(new double[] {at[0], at[1], BlastArt.hash(seed, 400 + i), zone.phase(), fade, i});
            }
        }
        flames.sort(Comparator.comparingDouble(f -> f[1]));
        for (double[] f : flames) {
            double flicker = 0.75 + 0.25 * Math.sin(f[3] * (9 + 3 * f[2]) + f[5] * 1.7)
                    + 0.08 * Math.sin(f[3] * 23 + f[5]);
            BlastArt.flame(g2, f[0], f[1], unit * (0.12 + 0.05 * f[2]), unit * (0.32 + 0.22 * f[2]) * flicker * f[4],
                    f[4]);
        }
        for (ZoneDraw zone : patch.discs()) {
            int seed = seedOf(zone);
            for (int i = 0; i < WISPS; i++) {
                double h = BlastArt.hash(seed, 500 + i);
                double k = (zone.phase() * 0.45 + h + i * 0.5) % 1;
                double sx = zone.centerX() + (h - 0.5) * zone.radius() + Math.sin(k * 5 + i) * unit * 0.08;
                double sy = zone.centerY() - unit * (0.25 + 0.9 * k);
                BlastArt.puff(g2, new BlastArt.Puff(sx, sy, unit * (0.1 + 0.16 * k), SMOKE,
                        0.4 * Math.sin(Math.PI * k) * zone.fade(), 0));
            }
        }
    }

    // ---- tar -------------------------------------------------------------------------------------------------------

    private static final int BUBBLES = 4;
    private static final double BUBBLE_SECONDS = 1.7;

    /** A thick, glossy pool: a sheen on its lit side, bubbles that swell and pop. */
    private static void tar(Graphics2D g2, ZonePatchDraw patch, Path2D ground, Path2D edge, int unit) {
        lie(g2, ground, edge, TAR_FILL, 0.88, TAR_EDGE, 0.6);
        for (ZoneDraw zone : patch.discs()) {
            int seed = seedOf(zone);
            double fade = zone.fade();
            double r = zone.radius();
            g2.setColor(BlastArt.faded(BlastArt.TAR.body(), 0.35 * fade));
            g2.fill(new Ellipse2D.Double(zone.centerX() - r * 0.66, zone.centerY() - r * 0.64, r * 1.0, r * 0.36));
            g2.setColor(BlastArt.faded(BlastArt.TAR.highlight(), 0.35 * fade));
            g2.fill(new Ellipse2D.Double(zone.centerX() - r * 0.5, zone.centerY() - r * 0.55, r * 0.42, r * 0.12));
            Stroke stroke = g2.getStroke();
            g2.setStroke(new BasicStroke(Math.max(1f, unit * 0.025f)));
            for (int i = 0; i < BUBBLES; i++) {
                double h = BlastArt.hash(seed, 600 + i);
                double k = (zone.phase() / BUBBLE_SECONDS + h + i * 0.25) % 1;
                double[] at = spot(zone, seed, i, BUBBLES, 0.65);
                double size = unit * (0.09 + 0.05 * h);
                if (k < 0.8) {
                    BlastArt.glob(g2, at[0], at[1], size * Math.sqrt(k / 0.8), fade);
                } else {
                    double pop = (k - 0.8) / 0.2;
                    g2.setColor(BlastArt.faded(BlastArt.TAR.highlight(), 0.7 * (1 - pop) * fade));
                    g2.draw(BlastArt.disc(at[0], at[1], size * (1 + 1.2 * pop)));
                }
            }
            g2.setStroke(stroke);
        }
    }

    // ---- frost -----------------------------------------------------------------------------------------------------

    private static final int FERNS = 6;
    private static final int SHARDS = 5;
    private static final int MIST = 3;
    private static final int GLINTS = 3;

    /** A sheet of frost: fern-like rime grown out from where the shell landed, shards of ice, a low mist drifting. */
    private static void frost(Graphics2D g2, ZonePatchDraw patch, Path2D ground, Path2D edge, int unit) {
        lie(g2, ground, edge, FROST_FILL, 0.45, FROST_EDGE, 0.8);
        g2.setStroke(new BasicStroke(Math.max(1f, unit * 0.022f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (ZoneDraw zone : patch.discs()) {
            int seed = seedOf(zone);
            g2.setColor(BlastArt.faded(RIME, 0.4 * zone.fade()));
            for (int i = 0; i < FERNS; i++) {
                double angle = i * Math.PI * 2 / FERNS + BlastArt.hash(seed, 700) * Math.PI;
                fern(g2, zone.centerX(), zone.centerY(), angle, zone.radius() * 0.78);
            }
        }
        List<double[]> shards = new ArrayList<>();
        for (ZoneDraw zone : patch.discs()) {
            int seed = seedOf(zone);
            for (int i = 0; i < SHARDS; i++) {
                double[] at = i == 0 ? new double[] {zone.centerX(), zone.centerY()} : spot(zone, seed, i, SHARDS, 0.5);
                shards.add(new double[] {at[0], at[1], BlastArt.hash(seed, 800 + i), i, zone.fade(),
                        (at[0] - zone.centerX()) / zone.radius()});
            }
        }
        shards.sort(Comparator.comparingDouble(s -> s[1]));
        for (double[] s : shards) {
            boolean middle = s[3] == 0;
            BlastArt.shard(g2, s[0], s[1], unit * (middle ? 0.13 : 0.08), unit * (middle ? 0.65 : 0.3 + 0.2 * s[2]),
                    s[5] * 0.6, s[4]);
        }
        for (ZoneDraw zone : patch.discs()) {
            int seed = seedOf(zone);
            for (int i = 0; i < MIST; i++) {
                double h = BlastArt.hash(seed, 900 + i);
                double k = (zone.phase() * 0.12 + h + i / (double) MIST) % 1;
                double mx = zone.centerX() + (k - 0.5) * zone.radius() * 1.3;
                double my = zone.centerY() + (h - 0.5) * zone.radius() * 0.9;
                BlastArt.flat(g2, mx, my, unit * (0.22 + 0.1 * h), Color.WHITE,
                        0.18 * Math.sin(Math.PI * k) * zone.fade());
            }
            for (int i = 0; i < GLINTS; i++) {
                double[] at = spot(zone, seed + 3, i, GLINTS, 0.8);
                double twinkle = Math.max(0, Math.sin(zone.phase() * 2.5 + i * 2.1 + seed));
                BlastArt.glint(g2, at[0], at[1], unit * 0.07 * twinkle, Color.WHITE, zone.fade());
            }
        }
    }

    /** A branch of rime: a line out from the centre with pairs of twigs off it. */
    private static void fern(Graphics2D g2, double x, double y, double angle, double length) {
        double ex = x + Math.cos(angle) * length;
        double ey = y + Math.sin(angle) * length;
        g2.draw(new Line2D.Double(x, y, ex, ey));
        for (double at : new double[] {0.4, 0.65, 0.85}) {
            double bx = x + Math.cos(angle) * length * at;
            double by = y + Math.sin(angle) * length * at;
            double twig = length * 0.22 * (1.1 - at);
            for (int side = -1; side <= 1; side += 2) {
                double a = angle + side * Math.PI / 4;
                g2.draw(new Line2D.Double(bx, by, bx + Math.cos(a) * twig, by + Math.sin(a) * twig));
            }
        }
    }
}
