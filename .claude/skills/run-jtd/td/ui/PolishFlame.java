package td.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.util.List;

/**
 * The Scorcher's flame, redrawn (docs/features/FEATURE-tower-visual-polish.md, "The flame"). Every stream of the frame is
 * painted in one pass, layer by layer, so where two cones cross their fire merges into one flame instead of stacking
 * its alpha twice. Over the fire, each enemy caught in a cone shows as a body standing in the jet: heat pooling on
 * its front, a dimmer wake behind it, its dark silhouette lit white-hot where the jet strikes and dull red round its
 * flanks, tongues of flame licking round both sides with a blue-violet plasma edge, and embers shed off it. How hot it all looks follows the fuel: a hotter stream and a hotter enemy burn yellower, then
 * white, then blue-white. Pure functions of their inputs and {@code flicker}: nothing is stored.
 */
final class PolishFlame {

    /**
     * One stream: its muzzle at {@code x, y} (board pixels), {@code heading}, {@code reach} pixels, {@code half} its
     * half-width in radians, {@code heat} from its fuel (0 the bare flame .. 1 every damage node; see {@link #heatOf}),
     * {@code coreHalf} the Core's half-width in radians (0 without a Core), {@code soul} Soulfire's violet, {@code
     * flicker} the game's flicker step.
     */
    record Stream(double x, double y, double heading, double reach, double half, double heat, double coreHalf,
                  boolean soul, int flicker) {
    }

    /**
     * An enemy in a cone: its outline already placed on the board (pixels), its centre, the heat it is fed (as the
     * stream's, more in the Core, more under Thermal Shock or Meltdown) and the nozzle it faces.
     */
    record Caught(Shape outline, double x, double y, double heat, double nozzleX, double nozzleY, boolean soul) {
    }

    /** The bare flame's fuel a second, the game's FLAME_POINTS: heat 0. */
    static final double BASE_FUEL = 60;
    /** Fuel a second at which a stream is as hot as it is drawn (heat 1): every Hot Flame and the White Flame. */
    static final double FULL_FUEL = 120;
    /** Heat added inside the Core, on top of the fuel the Core already adds. */
    static final double CORE_HEAT = 0.25;

    /** The three layers' alpha, the game's; the outer and middle merge across cones, the core stays each stream's. */
    static final float OUTER_ALPHA = 0.55f;
    static final float MIDDLE_ALPHA = 0.72f;
    static final float CORE_ALPHA = 0.88f;
    /** A silhouette: how dark it lies over the fire, its rim's width (cells) and how many wisps curl off it. */
    static final Color SILHOUETTE = new Color(0x1a0c06);
    static final float SILHOUETTE_ALPHA = 0.68f;
    static final double RIM_CELLS = 0.030;
    /** Tongues of flame licking round each flank, embers shed off it and how fast, the wake's length and depth. */
    static final int TONGUES = 2;
    static final int EMBERS = 4;
    static final double EMBER_SECONDS = 0.7;
    static final double WAKE_LENGTH = 3.0;
    static final float WAKE_ALPHA = 0.42f;
    static final Color PLASMA = new Color(0x7a6cff);
    static final Color PLASMA_SOUL = new Color(0x58d8ff);

    private PolishFlame() {
    }

    /** A stream's or an enemy's heat from the fuel it pours or takes a second. */
    static double heatOf(double fuelPerSecond) {
        return Math.max(0, (fuelPerSecond - BASE_FUEL) / (FULL_FUEL - BASE_FUEL));
    }

    private static Color faded(Color c, double a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) Math.round(Math.clamp(a, 0, 1) * 255));
    }

    private static Color mix(Color a, Color b, double t) {
        double s = Math.clamp(t, 0, 1);
        return new Color((int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * s),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * s),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * s));
    }

    /** A ramp through {@code stops} at {@code heat} 0, 1/(n-1), .. 1, held past its ends. */
    private static Color ramp(double heat, Color... stops) {
        double at = Math.clamp(heat, 0, 1) * (stops.length - 1);
        int i = Math.min(stops.length - 2, (int) Math.floor(at));
        return mix(stops[i], stops[i + 1], at - i);
    }

    static Color outer(double heat, boolean soul) {
        return soul ? ramp(heat, new Color(0x5a36b8), new Color(0x6a48d0))
                : ramp(heat, new Color(0xe2402a), new Color(0xf25a28), new Color(0xff6e2a));
    }

    static Color middle(double heat, boolean soul) {
        return soul ? ramp(heat, new Color(0x8f6cf0), new Color(0xb49cff))
                : ramp(heat, new Color(0xff8a2a), new Color(0xffa83a), new Color(0xffc24a));
    }

    static Color core(double heat, boolean soul) {
        return soul ? ramp(heat, new Color(0xd5e6ff), Color.WHITE)
                : ramp(heat, new Color(0xffe27a), new Color(0xfff2b8), new Color(0xfffaf0), new Color(0xe6f0ff));
    }

    /** The rim on a silhouette's hot edge: yellow, white-hot, then blue-white. */
    static Color rim(double heat, boolean soul) {
        return soul ? ramp(heat, new Color(0xe0d0ff), Color.WHITE)
                : ramp(heat / 1.25, new Color(0xffd84a), new Color(0xfff0a0), Color.WHITE, new Color(0xd8e8ff));
    }

    /** Every stream and every enemy caught in one, in one pass. */
    static void paint(Graphics2D g2, List<Stream> streams, List<Caught> caught, double scale, double seconds) {
        for (int soul = 0; soul < 2; soul++) {
            Area outer = new Area();
            Area middle = new Area();
            double heat = 0;
            int count = 0;
            for (Stream s : streams) {
                if (s.soul() != (soul == 1)) {
                    continue;
                }
                outer.add(new Area(placed(s, tongue(s.reach(), s.half(), 9, s.flicker() * 7))));
                middle.add(new Area(placed(s, tongue(s.reach() * 0.78, s.half() * 0.66, 7, s.flicker() * 7 + 1))));
                heat += s.heat();
                count++;
            }
            if (count == 0) {
                continue;
            }
            g2.setColor(faded(outer(heat / count, soul == 1), OUTER_ALPHA));
            g2.fill(outer);
            g2.setColor(faded(middle(heat / count, soul == 1), MIDDLE_ALPHA));
            g2.fill(middle);
        }
        for (Stream s : streams) {
            plasma(g2, s, scale);
            boolean core = s.coreHalf() > 0;
            double coreHalf = core ? s.coreHalf() : s.half() * 0.26;
            double coreHeat = s.heat() + (core ? CORE_HEAT : 0);
            g2.setColor(faded(core(coreHeat, s.soul()), CORE_ALPHA));
            g2.fill(placed(s, tongue(s.reach() * (core ? 0.72 : 0.42), coreHalf, 5, s.flicker() * 7 + 2)));
            if (coreHeat > 0.5) {
                g2.setColor(faded(Color.WHITE, Math.min(0.8, (coreHeat - 0.5) * 1.2)));
                g2.fill(placed(s, tongue(s.reach() * (core ? 0.5 : 0.28), coreHalf * 0.45, 3, s.flicker() * 7 + 3)));
            }
        }
        for (Caught c : caught) {
            silhouette(g2, c, scale, seconds);
        }
    }

    private static Shape placed(Stream s, Shape tongue) {
        AffineTransform t = AffineTransform.getTranslateInstance(s.x(), s.y());
        t.rotate(s.heading());
        return t.createTransformedShape(tongue);
    }

    /** As Java2DFrameRenderer.tongue: a flame along +x, its edge through tips and notches, shorter at its sides. */
    static Shape tongue(double reach, double half, int tips, int seed) {
        Path2D.Double path = new Path2D.Double();
        double root = reach * 0.01;
        path.moveTo(0, root);
        path.lineTo(0, -root);
        int points = tips * 2;
        for (int i = 0; i <= points; i++) {
            double angle = -half + 2 * half * i / points;
            double edge = 1 - 0.10 * Math.abs(angle / half);
            double roll = PolishLife.hash(seed, i);
            double share = i % 2 == 0 ? 0.78 + 0.10 * roll : 0.96 + 0.08 * roll;
            double r = reach * edge * share;
            path.lineTo(Math.cos(angle) * r, Math.sin(angle) * r);
        }
        path.closePath();
        return path;
    }

    /**
     * The plasma: a thin blue-violet sheen curving along each side of the cone from a third of its reach, brightest
     * mid-way, gone at the tips.
     */
    private static void plasma(Graphics2D g2, Stream s, double scale) {
        AffineTransform save = g2.getTransform();
        g2.translate(s.x(), s.y());
        g2.rotate(s.heading());
        Color tint = s.soul() ? PLASMA_SOUL : PLASMA;
        for (int side = -1; side <= 1; side += 2) {
            Path2D.Double sheen = new Path2D.Double();
            double inner = s.half() * 0.80;
            for (int i = 0; i <= 12; i++) {
                double u = 0.3 + 0.62 * i / 12.0;
                double a = side * inner * (1 - 0.15 * Math.sin(Math.PI * (u - 0.3) / 0.62));
                double r = s.reach() * u;
                if (i == 0) {
                    sheen.moveTo(Math.cos(a) * r, Math.sin(a) * r);
                } else {
                    sheen.lineTo(Math.cos(a) * r, Math.sin(a) * r);
                }
            }
            g2.setStroke(new BasicStroke((float) (scale * 0.05), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setPaint(new GradientPaint((float) (s.reach() * 0.3), 0f, faded(tint, 0),
                    (float) (s.reach() * 0.62), 0f, faded(tint, 0.30 + 0.15 * s.heat()), true));
            g2.draw(sheen);
        }
        g2.setTransform(save);
    }

    /**
     * An enemy in the fire, as a body standing in a jet of flame looks. The jet piles up against its front: a hot
     * glow pools on the side facing the nozzle. Its body blocks the fire: a dimmer wake trails behind it downstream.
     * Its dark silhouette is lit along the facing edge, white-hot where the jet strikes square and cooling to a dull
     * red round its flanks. Tongues of flame split on its front and lick round both sides, tapering and wavering as
     * they stream away, a faint blue-violet plasma along their outer edge; a few embers shed off its flanks.
     */
    private static void silhouette(Graphics2D g2, Caught c, double scale, double seconds) {
        double toNozzle = Math.atan2(c.nozzleY() - c.y(), c.nozzleX() - c.x());
        double down = toNozzle + Math.PI;
        double size = Math.max(c.outline().getBounds2D().getWidth(), c.outline().getBounds2D().getHeight()) / 2;
        int seed = (int) Math.round(c.x() * 7 + c.y() * 13);
        Color hot = rim(c.heat(), c.soul());
        Color flame = c.soul() ? new Color(0xb49cff) : new Color(0xff8a2a);
        Color plasma = c.soul() ? PLASMA_SOUL : PLASMA;
        double wakeX = c.x() + Math.cos(down) * size * WAKE_LENGTH;
        double wakeY = c.y() + Math.sin(down) * size * WAKE_LENGTH;
        Path2D.Double wake = new Path2D.Double();
        wake.moveTo(c.x() + Math.cos(toNozzle + Math.PI / 2) * size * 0.9,
                c.y() + Math.sin(toNozzle + Math.PI / 2) * size * 0.9);
        wake.quadTo(wakeX + Math.cos(toNozzle + Math.PI / 2) * size * 0.5,
                wakeY + Math.sin(toNozzle + Math.PI / 2) * size * 0.5, wakeX, wakeY);
        wake.quadTo(wakeX + Math.cos(toNozzle - Math.PI / 2) * size * 0.5,
                wakeY + Math.sin(toNozzle - Math.PI / 2) * size * 0.5,
                c.x() + Math.cos(toNozzle - Math.PI / 2) * size * 0.9,
                c.y() + Math.sin(toNozzle - Math.PI / 2) * size * 0.9);
        wake.closePath();
        g2.setPaint(new GradientPaint((float) c.x(), (float) c.y(), faded(SILHOUETTE, WAKE_ALPHA), (float) wakeX,
                (float) wakeY, faded(SILHOUETTE, 0)));
        g2.fill(wake);
        double frontX = c.x() + Math.cos(toNozzle) * size * 0.9;
        double frontY = c.y() + Math.sin(toNozzle) * size * 0.9;
        glowAt(g2, frontX, frontY, size * 1.25, hot, 0.55);
        glowAt(g2, frontX, frontY, size * 0.6, mix(hot, Color.WHITE, 0.4), 0.55);
        for (int side = -1; side <= 1; side += 2) {
            for (int k = 0; k < TONGUES; k++) {
                Shape t = lick(c.x(), c.y(), toNozzle, size, side, k, seconds, seed, 1.0);
                g2.setPaint(new GradientPaint((float) frontX, (float) frontY, faded(mix(flame, hot, 0.65), 0.95),
                        (float) (c.x() + Math.cos(down) * size * 3), (float) (c.y() + Math.sin(down) * size * 3),
                        faded(mix(flame, hot, 0.3), 0)));
                g2.fill(t);
                g2.setStroke(new BasicStroke((float) (scale * 0.008)));
                g2.setPaint(new GradientPaint((float) frontX, (float) frontY, faded(plasma, 0.45),
                        (float) (c.x() + Math.cos(down) * size * 2.5), (float) (c.y() + Math.sin(down) * size * 2.5),
                        faded(plasma, 0)));
                g2.draw(t);
            }
        }
        double backX = c.x() + Math.cos(down) * size;
        double backY = c.y() + Math.sin(down) * size;
        g2.setPaint(new GradientPaint((float) frontX, (float) frontY, faded(mix(SILHOUETTE, hot, 0.35), 0.62),
                (float) c.x(), (float) c.y(), faded(SILHOUETTE, SILHOUETTE_ALPHA)));
        g2.fill(c.outline());
        g2.setStroke(new BasicStroke((float) (RIM_CELLS * scale * 2.4), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setPaint(new GradientPaint((float) frontX, (float) frontY, faded(hot, 0.45), (float) backX, (float) backY,
                faded(new Color(0xc0301a), 0)));
        g2.draw(c.outline());
        g2.setStroke(new BasicStroke((float) (RIM_CELLS * scale * 0.9), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setPaint(new GradientPaint((float) frontX, (float) frontY, faded(mix(hot, Color.WHITE, 0.5), 1.0),
                (float) c.x(), (float) c.y(), faded(new Color(0xd04020), 0.15)));
        g2.draw(c.outline());
        for (int side = -1; side <= 1; side += 2) {
            Shape t = lick(c.x(), c.y(), toNozzle, size, side, 0, seconds, seed, 0.45);
            g2.setPaint(new GradientPaint((float) frontX, (float) frontY, faded(mix(hot, Color.WHITE, 0.3), 0.95),
                    (float) (c.x() + Math.cos(down) * size * 1.6), (float) (c.y() + Math.sin(down) * size * 1.6),
                    faded(hot, 0)));
            g2.fill(t);
        }
        for (int i = 0; i < EMBERS; i++) {
            double life = seconds / EMBER_SECONDS + (double) i / EMBERS + PolishLife.hash(seed, i) * 0.4;
            double age = life - Math.floor(life);
            int side = i % 2 == 0 ? 1 : -1;
            double flank = toNozzle + side * (Math.PI / 2 + 0.2);
            double ex = c.x() + Math.cos(flank) * size * (1.1 + 0.4 * age) + Math.cos(down) * size * 3.2 * age;
            double ey = c.y() + Math.sin(flank) * size * (1.1 + 0.4 * age) + Math.sin(down) * size * 3.2 * age;
            double r = scale * 0.012 * (1 - 0.6 * age);
            g2.setColor(faded(mix(mix(hot, Color.WHITE, 0.4), flame, age), (1 - age) * Math.min(1, age * 6)));
            g2.fill(new java.awt.geom.Ellipse2D.Double(ex - r, ey - r, r * 2, r * 2));
        }
    }

    /**
     * One tongue of flame licking round a body of radius {@code size} at {@code x, y}: it splits off the front a little
     * to {@code side} of the jet, hugs the flank, and streams away downstream, tapering to a point and wavering with
     * {@code seconds}. {@code k} picks one of the tongues on that side (later ones start further round and run
     * longer); {@code width} scales it (an inner, hotter tongue is narrower).
     */
    private static Shape lick(double x, double y, double toNozzle, double size, int side, int k, double seconds,
                              int seed, double width) {
        double down = toNozzle + Math.PI;
        double wobble = Math.sin(seconds * (11 + 4 * k) + seed * 0.37 + side * 1.9 + k * 2.1);
        double start = toNozzle + side * (0.25 + 0.38 * k);
        double flank = toNozzle + side * (Math.PI / 2 + 0.15);
        double length = size * (2.0 + 0.9 * k + 0.35 * wobble) * (0.85 + 0.3 * PolishLife.hash(seed + k, side + 3L));
        double ax = x + Math.cos(start) * size * 0.95;
        double ay = y + Math.sin(start) * size * 0.95;
        double bx = x + Math.cos(flank) * size * (1.12 + 0.08 * k);
        double by = y + Math.sin(flank) * size * (1.12 + 0.08 * k);
        double ex = bx + Math.cos(down + side * 0.18) * length;
        double ey = by + Math.sin(down + side * 0.18) * length;
        int n = 14;
        double[][] left = new double[n + 1][];
        double[][] right = new double[n + 1][];
        double root = size * 0.34 * width * (1 - 0.25 * k);
        for (int i = 0; i <= n; i++) {
            double t = (double) i / n;
            double px = (1 - t) * (1 - t) * ax + 2 * (1 - t) * t * bx + t * t * ex;
            double py = (1 - t) * (1 - t) * ay + 2 * (1 - t) * t * by + t * t * ey;
            double dx = 2 * (1 - t) * (bx - ax) + 2 * t * (ex - bx);
            double dy = 2 * (1 - t) * (by - ay) + 2 * t * (ey - by);
            double len = Math.max(1e-6, Math.hypot(dx, dy));
            double nx = -dy / len;
            double ny = dx / len;
            double sway = Math.sin(seconds * 13 + t * 7 + k * 1.3 + seed) * size * 0.12 * t;
            px += nx * sway;
            py += ny * sway;
            double w = root * Math.pow(1 - t, 0.8) * (0.55 + 0.45 * Math.sin(Math.PI * Math.min(1, t * 1.6 + 0.2)));
            left[i] = new double[] {px + nx * w, py + ny * w};
            right[i] = new double[] {px - nx * w, py - ny * w};
        }
        Path2D.Double p = new Path2D.Double();
        p.moveTo(left[0][0], left[0][1]);
        for (int i = 1; i <= n; i++) {
            p.lineTo(left[i][0], left[i][1]);
        }
        for (int i = n; i >= 0; i--) {
            p.lineTo(right[i][0], right[i][1]);
        }
        p.closePath();
        return p;
    }

    private static void glowAt(Graphics2D g2, double x, double y, double r, Color c, double alpha) {
        g2.setPaint(new java.awt.RadialGradientPaint((float) x, (float) y, (float) r, new float[] {0f, 1f},
                new Color[] {faded(c, alpha), faded(c, 0)}));
        g2.fill(new java.awt.geom.Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
    }
}
