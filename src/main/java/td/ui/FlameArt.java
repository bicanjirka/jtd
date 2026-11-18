package td.ui;

import td.ui.render.FlameDraw;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

/**
 * The Scorchers' flame. Every stream of the frame is painted in one pass, layer by layer, so where two cones cross their
 * fire merges into one flame instead of stacking its alpha twice. Over the fire, each enemy caught in a cone shows as a
 * body standing in the jet: heat pooling on its front, a dimmer wake behind it, its dark silhouette lit white-hot where
 * the jet strikes and dull red round its flanks, tongues of flame licking round both sides with a blue-violet plasma
 * edge, and embers shed off it. How hot it all looks follows the fuel: a hotter stream and a hotter enemy burn yellower,
 * then white, then blue-white. Nothing is stored between frames.
 */
final class FlameArt {

    /** Fuel a second at which a stream is as hot as it is drawn: every damage node it can take. */
    static final double FULL_FUEL = 120;
    /** Heat added inside the Core, on top of the fuel the Core already adds. */
    static final double CORE_HEAT = 0.25;

    /** The three layers' alpha; the outer and middle merge across cones, the core stays each stream's. */
    private static final float OUTER_ALPHA = 0.55f;
    private static final float MIDDLE_ALPHA = 0.72f;
    private static final float CORE_ALPHA = 0.88f;
    private static final int OUTER_TIPS = 9;
    private static final int MIDDLE_TIPS = 7;
    private static final int CORE_TIPS = 5;
    private static final int WHITE_TIPS = 3;
    private static final int TONGUE_SEEDS = 7;
    /** A silhouette: how dark it lies over the fire, and its rim's width in cells. */
    private static final Color SILHOUETTE = new Color(0x1a0c06);
    private static final float SILHOUETTE_ALPHA = 0.68f;
    private static final double RIM_CELLS = 0.030;
    /** Tongues of flame licking round each flank, embers shed off it and how fast, the wake's length and depth. */
    private static final int TONGUES = 2;
    private static final int EMBERS = 4;
    private static final double EMBER_SECONDS = 0.7;
    private static final double WAKE_LENGTH = 3.0;
    private static final float WAKE_ALPHA = 0.42f;
    private static final int LICK_POINTS = 14;
    private static final int PLASMA_POINTS = 12;
    private static final Color PLASMA = new Color(0x7a6cff);
    private static final Color PLASMA_SOUL = new Color(0x58d8ff);
    private static final Color FLAME = new Color(0xff8a2a);
    private static final Color FLAME_SOUL = new Color(0xb49cff);
    private static final Color RIM_COOL = new Color(0xc0301a);
    private static final Color RIM_DULL = new Color(0xd04020);

    private FlameArt() {
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

    private static Color outer(double heat, boolean soul) {
        return soul ? ramp(heat, new Color(0x5a36b8), new Color(0x6a48d0))
                : ramp(heat, new Color(0xe2402a), new Color(0xf25a28), new Color(0xff6e2a));
    }

    private static Color middle(double heat, boolean soul) {
        return soul ? ramp(heat, new Color(0x8f6cf0), new Color(0xb49cff))
                : ramp(heat, new Color(0xff8a2a), new Color(0xffa83a), new Color(0xffc24a));
    }

    private static Color core(double heat, boolean soul) {
        return soul ? ramp(heat, new Color(0xd5e6ff), Color.WHITE)
                : ramp(heat, new Color(0xffe27a), new Color(0xfff2b8), new Color(0xfffaf0), new Color(0xe6f0ff));
    }

    /** The rim on a silhouette's hot edge: yellow, white-hot, then blue-white. */
    private static Color rim(double heat, boolean soul) {
        return soul ? ramp(heat, new Color(0xe0d0ff), Color.WHITE)
                : ramp(heat / 1.25, new Color(0xffd84a), new Color(0xfff0a0), Color.WHITE, new Color(0xd8e8ff));
    }

    /** Every stream and every enemy caught in one. */
    static void paint(Graphics2D g2, FlameDraw flame) {
        double scale = flame.cellPixels();
        // Every tongue winds the same way, so a non-zero fill of all of them is their union, filled once.
        for (int soul = 0; soul < 2; soul++) {
            Path2D.Double outer = new Path2D.Double(Path2D.WIND_NON_ZERO);
            Path2D.Double middle = new Path2D.Double(Path2D.WIND_NON_ZERO);
            double heat = 0;
            int count = 0;
            for (FlameDraw.Stream s : flame.streams()) {
                if (s.soulfire() != (soul == 1)) {
                    continue;
                }
                int seed = s.flicker() * TONGUE_SEEDS;
                outer.append(placed(s, tongue(s.reach(), s.halfWidthRadians(), OUTER_TIPS, seed)), false);
                middle.append(placed(s, tongue(s.reach() * 0.78, s.halfWidthRadians() * 0.66, MIDDLE_TIPS, seed + 1)),
                        false);
                heat += s.heat();
                count++;
            }
            if (count == 0) {
                continue;
            }
            g2.setColor(TowerLifeArt.faded(outer(heat / count, soul == 1), OUTER_ALPHA));
            g2.fill(outer);
            g2.setColor(TowerLifeArt.faded(middle(heat / count, soul == 1), MIDDLE_ALPHA));
            g2.fill(middle);
        }
        for (FlameDraw.Stream s : flame.streams()) {
            plasma(g2, s, scale);
            boolean core = s.coreHalfWidthRadians() > 0;
            double coreHalf = core ? s.coreHalfWidthRadians() : s.halfWidthRadians() * 0.26;
            double coreHeat = s.heat() + (core ? CORE_HEAT : 0);
            int seed = s.flicker() * TONGUE_SEEDS;
            g2.setColor(TowerLifeArt.faded(core(coreHeat, s.soulfire()), CORE_ALPHA));
            g2.fill(placed(s, tongue(s.reach() * (core ? 0.72 : 0.42), coreHalf, CORE_TIPS, seed + 2)));
            if (coreHeat > 0.5) {
                g2.setColor(TowerLifeArt.faded(Color.WHITE, Math.min(0.8, (coreHeat - 0.5) * 1.2)));
                g2.fill(placed(s, tongue(s.reach() * (core ? 0.5 : 0.28), coreHalf * 0.45, WHITE_TIPS, seed + 3)));
            }
        }
        for (FlameDraw.Caught c : flame.caught()) {
            silhouette(g2, c, scale, flame.seconds());
        }
    }

    private static Shape placed(FlameDraw.Stream s, Shape tongue) {
        AffineTransform t = AffineTransform.getTranslateInstance(s.x(), s.y());
        t.rotate(s.headingRadians());
        return t.createTransformedShape(tongue);
    }

    /** A flame along +x, its edge through tips and notches across its width, a little shorter toward its sides. */
    private static Shape tongue(double reach, double half, int tips, int seed) {
        Path2D.Double path = new Path2D.Double();
        double root = reach * 0.01;
        path.moveTo(0, root);
        path.lineTo(0, -root);
        int points = tips * 2;
        for (int i = 0; i <= points; i++) {
            double angle = -half + 2 * half * i / points;
            double edge = 1 - 0.10 * Math.abs(angle / half);
            double roll = TowerLifeArt.hash(seed, i);
            double share = i % 2 == 0 ? 0.78 + 0.10 * roll : 0.96 + 0.08 * roll;
            double r = reach * edge * share;
            path.lineTo(Math.cos(angle) * r, Math.sin(angle) * r);
        }
        path.closePath();
        return path;
    }

    /** A thin blue-violet sheen curving along each side of the cone from a third of its reach, gone at the tips. */
    private static void plasma(Graphics2D g2, FlameDraw.Stream s, double scale) {
        AffineTransform save = g2.getTransform();
        g2.translate(s.x(), s.y());
        g2.rotate(s.headingRadians());
        Color tint = s.soulfire() ? PLASMA_SOUL : PLASMA;
        g2.setStroke(new BasicStroke((float) (scale * 0.05), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setPaint(new GradientPaint(s.reach() * 0.3f, 0f, TowerLifeArt.faded(tint, 0), s.reach() * 0.62f, 0f,
                TowerLifeArt.faded(tint, 0.30 + 0.15 * s.heat()), true));
        for (int side = -1; side <= 1; side += 2) {
            Path2D.Double sheen = new Path2D.Double();
            double inner = s.halfWidthRadians() * 0.80;
            for (int i = 0; i <= PLASMA_POINTS; i++) {
                double u = 0.3 + 0.62 * i / PLASMA_POINTS;
                double a = side * inner * (1 - 0.15 * Math.sin(Math.PI * (u - 0.3) / 0.62));
                double r = s.reach() * u;
                if (i == 0) {
                    sheen.moveTo(Math.cos(a) * r, Math.sin(a) * r);
                } else {
                    sheen.lineTo(Math.cos(a) * r, Math.sin(a) * r);
                }
            }
            g2.draw(sheen);
        }
        g2.setTransform(save);
    }


    /**
     * An enemy in the fire. The jet piles up against its front: a hot glow pools on the side facing the nozzle. Its
     * body blocks the fire: a dimmer wake trails behind it downstream. Its dark silhouette is lit along the facing edge,
     * white-hot where the jet strikes square and cooling to a dull red round its flanks. Tongues of flame split on its
     * front and lick round both sides, a faint plasma along their outer edge; a few embers shed off its flanks.
     */
    private static void silhouette(Graphics2D g2, FlameDraw.Caught c, double scale, double seconds) {
        double x = c.x();
        double y = c.y() - EnemySprites.lift(c.look(), c.x(), c.y(), c.bodyScale());
        AffineTransform place = AffineTransform.getTranslateInstance(x, y);
        place.rotate(c.facingRadians());
        Shape outline = place.createTransformedShape(EnemyLooks.of(c.look()).body().at(c.bodyScale()));
        double toNozzle = Math.atan2(c.nozzleY() - y, c.nozzleX() - x);
        double down = toNozzle + Math.PI;
        double size = Math.max(outline.getBounds2D().getWidth(), outline.getBounds2D().getHeight()) / 2;
        int seed = (int) Math.round(c.x() * 7 + c.y() * 13);
        Color hot = rim(c.heat(), c.soulfire());
        Color flame = c.soulfire() ? FLAME_SOUL : FLAME;
        Color plasma = c.soulfire() ? PLASMA_SOUL : PLASMA;
        double across = toNozzle + Math.PI / 2;
        double wakeX = x + Math.cos(down) * size * WAKE_LENGTH;
        double wakeY = y + Math.sin(down) * size * WAKE_LENGTH;
        Path2D.Double wake = new Path2D.Double();
        wake.moveTo(x + Math.cos(across) * size * 0.9, y + Math.sin(across) * size * 0.9);
        wake.quadTo(wakeX + Math.cos(across) * size * 0.5, wakeY + Math.sin(across) * size * 0.5, wakeX, wakeY);
        wake.quadTo(wakeX - Math.cos(across) * size * 0.5, wakeY - Math.sin(across) * size * 0.5,
                x - Math.cos(across) * size * 0.9, y - Math.sin(across) * size * 0.9);
        wake.closePath();
        g2.setPaint(new GradientPaint((float) x, (float) y, TowerLifeArt.faded(SILHOUETTE, WAKE_ALPHA), (float) wakeX,
                (float) wakeY, TowerLifeArt.faded(SILHOUETTE, 0)));
        g2.fill(wake);
        double frontX = x + Math.cos(toNozzle) * size * 0.9;
        double frontY = y + Math.sin(toNozzle) * size * 0.9;
        TowerLifeArt.glow(g2, frontX, frontY, size * 1.25, hot, 0.55);
        TowerLifeArt.glow(g2, frontX, frontY, size * 0.6, mix(hot, Color.WHITE, 0.4), 0.55);
        g2.setStroke(new BasicStroke((float) (scale * 0.008)));
        for (int side = -1; side <= 1; side += 2) {
            for (int k = 0; k < TONGUES; k++) {
                Shape t = lick(x, y, toNozzle, size, side, k, seconds, seed, 1.0);
                g2.setPaint(new GradientPaint((float) frontX, (float) frontY,
                        TowerLifeArt.faded(mix(flame, hot, 0.65), 0.95), (float) (x + Math.cos(down) * size * 3),
                        (float) (y + Math.sin(down) * size * 3), TowerLifeArt.faded(mix(flame, hot, 0.3), 0)));
                g2.fill(t);
                g2.setPaint(new GradientPaint((float) frontX, (float) frontY, TowerLifeArt.faded(plasma, 0.45),
                        (float) (x + Math.cos(down) * size * 2.5), (float) (y + Math.sin(down) * size * 2.5),
                        TowerLifeArt.faded(plasma, 0)));
                g2.draw(t);
            }
        }
        double backX = x + Math.cos(down) * size;
        double backY = y + Math.sin(down) * size;
        g2.setPaint(new GradientPaint((float) frontX, (float) frontY,
                TowerLifeArt.faded(mix(SILHOUETTE, hot, 0.35), 0.62), (float) x, (float) y,
                TowerLifeArt.faded(SILHOUETTE, SILHOUETTE_ALPHA)));
        g2.fill(outline);
        g2.setStroke(new BasicStroke((float) (RIM_CELLS * scale * 2.4), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setPaint(new GradientPaint((float) frontX, (float) frontY, TowerLifeArt.faded(hot, 0.45), (float) backX,
                (float) backY, TowerLifeArt.faded(RIM_COOL, 0)));
        g2.draw(outline);
        g2.setStroke(new BasicStroke((float) (RIM_CELLS * scale * 0.9), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setPaint(new GradientPaint((float) frontX, (float) frontY, TowerLifeArt.faded(mix(hot, Color.WHITE, 0.5), 1.0),
                (float) x, (float) y, TowerLifeArt.faded(RIM_DULL, 0.15)));
        g2.draw(outline);
        for (int side = -1; side <= 1; side += 2) {
            Shape t = lick(x, y, toNozzle, size, side, 0, seconds, seed, 0.45);
            g2.setPaint(new GradientPaint((float) frontX, (float) frontY,
                    TowerLifeArt.faded(mix(hot, Color.WHITE, 0.3), 0.95), (float) (x + Math.cos(down) * size * 1.6),
                    (float) (y + Math.sin(down) * size * 1.6), TowerLifeArt.faded(hot, 0)));
            g2.fill(t);
        }
        for (int i = 0; i < EMBERS; i++) {
            double life = seconds / EMBER_SECONDS + (double) i / EMBERS + TowerLifeArt.hash(seed, i) * 0.4;
            double age = life - Math.floor(life);
            int side = i % 2 == 0 ? 1 : -1;
            double flank = toNozzle + side * (Math.PI / 2 + 0.2);
            double ex = x + Math.cos(flank) * size * (1.1 + 0.4 * age) + Math.cos(down) * size * 3.2 * age;
            double ey = y + Math.sin(flank) * size * (1.1 + 0.4 * age) + Math.sin(down) * size * 3.2 * age;
            double r = scale * 0.012 * (1 - 0.6 * age);
            g2.setColor(TowerLifeArt.faded(mix(mix(hot, Color.WHITE, 0.4), flame, age),
                    (1 - age) * Math.min(1, age * 6)));
            g2.fill(new Ellipse2D.Double(ex - r, ey - r, r * 2, r * 2));
        }
    }

    /**
     * One tongue of flame licking round a body of radius {@code size} at {@code x, y}: it splits off the front a little
     * to {@code side} of the jet, hugs the flank, and streams away downstream, tapering to a point and wavering.
     * {@code k} picks one of the tongues on that side (later ones start further round and run longer); {@code width}
     * scales it (an inner, hotter tongue is narrower).
     */
    private static Shape lick(double x, double y, double toNozzle, double size, int side, int k, double seconds,
                              int seed, double width) {
        double down = toNozzle + Math.PI;
        double wobble = Math.sin(seconds * (11 + 4 * k) + seed * 0.37 + side * 1.9 + k * 2.1);
        double start = toNozzle + side * (0.25 + 0.38 * k);
        double flank = toNozzle + side * (Math.PI / 2 + 0.15);
        double length = size * (2.0 + 0.9 * k + 0.35 * wobble)
                * (0.85 + 0.3 * TowerLifeArt.hash(seed + k, side + 3L));
        double ax = x + Math.cos(start) * size * 0.95;
        double ay = y + Math.sin(start) * size * 0.95;
        double bx = x + Math.cos(flank) * size * (1.12 + 0.08 * k);
        double by = y + Math.sin(flank) * size * (1.12 + 0.08 * k);
        double ex = bx + Math.cos(down + side * 0.18) * length;
        double ey = by + Math.sin(down + side * 0.18) * length;
        double[] leftX = new double[LICK_POINTS + 1];
        double[] leftY = new double[LICK_POINTS + 1];
        double[] rightX = new double[LICK_POINTS + 1];
        double[] rightY = new double[LICK_POINTS + 1];
        double root = size * 0.34 * width * (1 - 0.25 * k);
        for (int i = 0; i <= LICK_POINTS; i++) {
            double t = (double) i / LICK_POINTS;
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
            leftX[i] = px + nx * w;
            leftY[i] = py + ny * w;
            rightX[i] = px - nx * w;
            rightY[i] = py - ny * w;
        }
        Path2D.Double p = new Path2D.Double();
        p.moveTo(leftX[0], leftY[0]);
        for (int i = 1; i <= LICK_POINTS; i++) {
            p.lineTo(leftX[i], leftY[i]);
        }
        for (int i = LICK_POINTS; i >= 0; i--) {
            p.lineTo(rightX[i], rightY[i]);
        }
        p.closePath();
        return p;
    }
}
