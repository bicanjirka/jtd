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
 * The Scorchers' flame. Every stream of the frame is painted in one pass, layer by layer, so where two cones of one
 * fire cross they merge into one flame instead of stacking their alpha twice. A cone's solid fire covers its whole
 * reach, its front licking past it; inside it burn a middle tongue and a core tongue, small on a bare tower and grown to
 * the bands its nodes give it, though shorter than the reach they pour to. Blue Flame turns the middle white and the
 * core blue. Each fire has its own look: fire licks in jagged tips and sheds sparks, Dragon's Breath rolls in deep red
 * billows and smoke, Soulfire ripples violet in a ghostly halo and lets off pale wisps.
 * <p>
 * Over the fire, each enemy caught in a cone shows as a body standing in the jet: heat pooling on its front, a dimmer
 * wake behind it, its dark silhouette ringed by a rim of its heat, tongues of flame licking round both sides and embers
 * shed off it. How hot it all looks follows the fuel. Nothing is stored between frames.
 */
final class FlameArt {

    /** Fuel a second at which a stream is as hot as it is drawn: every damage node it can take. */
    static final double FULL_FUEL = 105;
    /** Heat added inside the Core, on top of the fuel the Core already adds. */
    static final double CORE_HEAT = 0.25;
    /** Heat each cone past the hottest adds to an enemy caught in several. */
    static final double CROSSFIRE_HEAT = 0.1;

    private static final int OUTER_TIPS = 9;
    private static final int TONGUE_SEEDS = 7;
    /** The tongues of a bare tower, as shares of the cone's half-width and reach. */
    private static final double BARE_MIDDLE_WIDTH = 0.42;
    private static final double BARE_MIDDLE_REACH = 0.55;
    private static final double BARE_CORE_WIDTH = 0.13;
    private static final double BARE_CORE_REACH = 0.32;
    /** With the Core the middle tongue grows too, so it still holds the core's; how far each band's tongue reaches. */
    private static final double CORED_MIDDLE_WIDTH = 0.5;
    private static final double CORED_MIDDLE_REACH = 0.78;
    private static final double MIDDLE_REACH = 0.82;
    private static final double CORE_REACH = 0.7;
    /** Dragon's Breath's and Soulfire's halo, past their cone. */
    private static final double HALO_REACH = 1.07;
    private static final double HALO_WIDTH = 1.1;
    /** A silhouette: how dark it lies over the fire, and its coolest rim's width in cells. */
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
    private static final Color FLAME = new Color(0xff8a2a);
    private static final Color FLAME_SOUL = new Color(0xb49cff);
    private static final Color SMOKE = new Color(0x1e1412);
    private static final Color WISP = new Color(0xe6f8ff);
    private static final Color WISP_GLOW = new Color(0x9fe8ff);

    private FlameArt() {
    }

    /** The three fires a stream can be: its own, Dragon's Breath and Soulfire. */
    private enum Fire {
        FLAME, DRAGON, SOUL
    }

    private static Fire fireOf(FlameDraw.Stream s) {
        return s.soulfire() ? Fire.SOUL : s.dragon() ? Fire.DRAGON : Fire.FLAME;
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

    private static Color outer(Fire fire, double heat) {
        return switch (fire) {
            case FLAME -> ramp(heat, new Color(0xe2402a), new Color(0xf25a28), new Color(0xff6e2a));
            case DRAGON -> ramp(heat, new Color(0xb3201a), new Color(0xcf3119), new Color(0xe5461c));
            case SOUL -> ramp(heat, new Color(0x4a2aa0), new Color(0x5a36b8), new Color(0x6a48d0));
        };
    }

    private static float outerAlpha(Fire fire) {
        return switch (fire) {
            case FLAME -> 0.55f;
            case DRAGON -> 0.62f;
            case SOUL -> 0.5f;
        };
    }

    private static Color middle(Fire fire, double heat) {
        return switch (fire) {
            case FLAME -> ramp(heat, new Color(0xff8a2a), new Color(0xffa83a), new Color(0xffc24a));
            case DRAGON -> ramp(heat, new Color(0xff951c), new Color(0xffb534), new Color(0xffcf58));
            case SOUL -> ramp(heat, new Color(0x8f6cf0), new Color(0xa486f8), new Color(0xb49cff));
        };
    }

    /** The middle under Blue Flame: white, warm in fire, cool in Soulfire. */
    private static Color white(Fire fire) {
        return switch (fire) {
            case FLAME -> new Color(0xfff6e6);
            case DRAGON -> new Color(0xfff1d0);
            case SOUL -> new Color(0xf0eaff);
        };
    }

    /** The core: from warm to white as it heats, light blue once strong, and wholly blue under a white middle. */
    private static Color core(Fire fire, double heat, boolean whiteMiddle) {
        Color warm = switch (fire) {
            case FLAME -> new Color(0xffe27a);
            case DRAGON -> new Color(0xffe066);
            case SOUL -> new Color(0xc8d6ff);
        };
        Color blue = fire == Fire.SOUL ? new Color(0x5fd8f5) : new Color(0x62b2f5);
        return ramp(whiteMiddle ? 1 : heat, warm, new Color(0xfff6d8), new Color(0xfffcf4), blue);
    }

    /** How far {@code heat} is toward the hottest a rim gets: a full stream in the Core. */
    private static double rimShare(double heat) {
        return Math.clamp(heat / (1 + CORE_HEAT), 0, 1);
    }

    /** The rim round a silhouette: orange-red, yellow, white-hot, then ice-blue. */
    private static Color rim(double heat, boolean soul) {
        return soul ? ramp(rimShare(heat), new Color(0xa47cff), new Color(0xe0d0ff), Color.WHITE, new Color(0xa8f0ff))
                : ramp(rimShare(heat), new Color(0xe04a20), new Color(0xffd040), Color.WHITE, new Color(0x9cd0ff));
    }

    /** Every stream and every enemy caught in one. */
    static void paint(Graphics2D g2, FlameDraw flame) {
        double scale = flame.cellPixels();
        double t = flame.seconds();
        for (Fire fire : Fire.values()) {
            // Every shape winds the same way, so a non-zero fill of a layer is the union of every stream's, filled once.
            Path2D.Double halo = new Path2D.Double(Path2D.WIND_NON_ZERO);
            Path2D.Double outer = new Path2D.Double(Path2D.WIND_NON_ZERO);
            Path2D.Double middle = new Path2D.Double(Path2D.WIND_NON_ZERO);
            Path2D.Double core = new Path2D.Double(Path2D.WIND_NON_ZERO);
            double heat = 0;
            double coreHeat = 0;
            int count = 0;
            boolean whiteMiddle = false;
            for (FlameDraw.Stream s : flame.streams()) {
                if (fireOf(s) != fire) {
                    continue;
                }
                int seed = seedOf(s);
                double half = s.halfWidthRadians();
                outer.append(placed(s, front(fire, s.inner(), s.reach(), half, s.flicker(), seed, t, 1)), false);
                if (fire != Fire.FLAME) {
                    halo.append(placed(s, front(fire, s.inner(), s.reach() * HALO_REACH, half * HALO_WIDTH,
                            s.flicker(), seed, t, 1.3)), false);
                }
                boolean white = s.middleHalfWidthRadians() > 0;
                boolean cored = s.coreHalfWidthRadians() > 0;
                double middleHalf = white ? s.middleHalfWidthRadians()
                        : half * (cored ? CORED_MIDDLE_WIDTH : BARE_MIDDLE_WIDTH);
                double middleReach = white ? MIDDLE_REACH : cored ? CORED_MIDDLE_REACH : BARE_MIDDLE_REACH;
                double coreHalf = cored ? s.coreHalfWidthRadians() : half * BARE_CORE_WIDTH;
                double coreReach = cored ? CORE_REACH : BARE_CORE_REACH;
                middle.append(placed(s, tongue(fire, s.inner(), s.reach() * middleReach, middleHalf, seed + 1, t)),
                        false);
                core.append(placed(s, tongue(fire, s.inner(), s.reach() * coreReach, coreHalf, seed + 2, t)), false);
                whiteMiddle |= white;
                heat += s.heat();
                coreHeat += s.coreHeat() + (cored ? CORE_HEAT : 0);
                count++;
            }
            if (count == 0) {
                continue;
            }
            Color outerColor = outer(fire, heat / count);
            if (fire != Fire.FLAME) {
                g2.setColor(TowerLifeArt.faded(outerColor, fire == Fire.SOUL ? 0.24 : 0.2));
                g2.fill(halo);
            }
            g2.setColor(TowerLifeArt.faded(outerColor, outerAlpha(fire)));
            g2.fill(outer);
            g2.setColor(TowerLifeArt.faded(whiteMiddle ? white(fire) : middle(fire, heat / count),
                    whiteMiddle ? 0.84 : 0.74));
            g2.fill(middle);
            g2.setColor(TowerLifeArt.faded(core(fire, coreHeat / count, whiteMiddle), 0.9));
            g2.fill(core);
        }
        for (FlameDraw.Stream s : flame.streams()) {
            motes(g2, s, fireOf(s), scale, t);
        }
        for (FlameDraw.Caught c : flame.caught()) {
            silhouette(g2, c, scale, t);
        }
    }

    private static int seedOf(FlameDraw.Stream s) {
        return (int) Math.round(s.x() * 7 + s.y() * 13);
    }

    private static Shape placed(FlameDraw.Stream s, Shape shape) {
        AffineTransform at = AffineTransform.getTranslateInstance(s.x(), s.y());
        at.rotate(s.headingRadians());
        return at.createTransformedShape(shape);
    }

    /**
     * The cone along +x from its apex, from {@code inner} out to {@code reach}, {@code half} either side: the wedge it
     * pours into, its front the fire's own. Fire licks past the reach in jagged tips, Dragon's Breath rolls in round
     * billows, Soulfire ripples; {@code soft} stretches how far.
     */
    private static Shape front(Fire fire, double inner, double reach, double half, int flicker, int seed, double t,
                               double soft) {
        Path2D.Double path = new Path2D.Double();
        int arc = 10;
        for (int i = 0; i <= arc; i++) {
            double a = half - 2 * half * i / arc;
            if (i == 0) {
                path.moveTo(Math.cos(a) * inner, Math.sin(a) * inner);
            } else {
                path.lineTo(Math.cos(a) * inner, Math.sin(a) * inner);
            }
        }
        if (fire == Fire.FLAME) {
            int points = OUTER_TIPS * 2;
            int tipSeed = flicker * TONGUE_SEEDS + seed;
            for (int i = 0; i <= points; i++) {
                double a = -half + 2 * half * i / points;
                double roll = TowerLifeArt.hash(tipSeed, i);
                double over = (0.15 + 0.10 * roll) * (1 - 0.5 * Math.abs(a / half)) * soft;
                double r = reach * (i % 2 == 0 ? 1.0 + 0.04 * roll : 1.0 + over);
                path.lineTo(Math.cos(a) * r, Math.sin(a) * r);
            }
        } else {
            int points = 48;
            for (int i = 0; i <= points; i++) {
                double u = (double) i / points;
                double a = -half + 2 * half * u;
                double side = 1 - 0.45 * Math.abs(a / half);
                double out = fire == Fire.DRAGON
                        ? 0.15 * Math.pow(Math.abs(Math.sin(Math.PI * (5.5 * u - t * 1.6) + seed)), 0.55) * side
                        : (0.06 + 0.05 * Math.sin(u * 19 + t * 2.4 + seed) + 0.03 * Math.sin(u * 41 - t * 3.3)) * side;
                double r = reach * (1 + out * soft);
                path.lineTo(Math.cos(a) * r, Math.sin(a) * r);
            }
        }
        path.closePath();
        return path;
    }

    /**
     * A tongue of flame inside the cone, from the nozzle at {@code inner} to its tip at {@code end}: as wide as
     * {@code half} near its root, tapering to a point, its sides wavering and its length pulsing. Soulfire sways slow
     * and wide, Dragon's Breath hardly at all.
     */
    private static Shape tongue(Fire fire, double inner, double end, double half, int seed, double t) {
        int n = 22;
        double length = Math.max(end - inner, 1);
        double wave = switch (fire) {
            case FLAME -> 0.09;
            case DRAGON -> 0.05;
            case SOUL -> 0.16;
        };
        double speed = fire == Fire.SOUL ? 3.0 : 9.0;
        double pulse = 0.05 * Math.sin(t * (fire == Fire.SOUL ? 2.1 : 6.3) + seed);
        double sway = (fire == Fire.SOUL ? 0.22 : 0.08) * half * Math.sin(t * 2.7 + seed);
        double[] xs = new double[n + 1];
        double[] ws = new double[n + 1];
        for (int i = 0; i <= n; i++) {
            double u = (double) i / n;
            xs[i] = inner + length * (1 + pulse) * u;
            ws[i] = Math.max(0, xs[i] * Math.tan(half) * Math.pow(1 - u, 0.6) * (1 + wave * Math.sin(u * 9 - t * speed
                    + seed)));
        }
        Path2D.Double path = new Path2D.Double();
        path.moveTo(xs[0], -ws[0]);
        for (int i = 1; i <= n; i++) {
            path.lineTo(xs[i], -ws[i] + xs[i] * sway * i / n);
        }
        for (int i = n; i >= 0; i--) {
            path.lineTo(xs[i], ws[i] + xs[i] * sway * i / n);
        }
        path.closePath();
        return path;
    }

    /**
     * What a stream throws off, past a glow on its nozzle: fire sheds sparks that fly past its reach, Dragon's Breath
     * rolls dark smoke off its front, Soulfire lets pale wisps curl up out of it. Each mote follows the clock.
     */
    private static void motes(Graphics2D g2, FlameDraw.Stream s, Fire fire, double scale, double t) {
        int seed = seedOf(s);
        double hx = Math.cos(s.headingRadians());
        double hy = Math.sin(s.headingRadians());
        TowerLifeArt.glow(g2, s.x() + hx * s.inner(), s.y() + hy * s.inner(), scale * 0.22, core(fire, s.heat(), false),
                0.6);
        int motes = fire == Fire.FLAME ? 10 : 6;
        double rate = switch (fire) {
            case FLAME -> 1.3;
            case DRAGON -> 0.6;
            case SOUL -> 0.45;
        };
        for (int i = 0; i < motes; i++) {
            double life = t * rate + TowerLifeArt.hash(seed, i);
            double age = life - Math.floor(life);
            long generation = (long) Math.floor(life);
            double a = s.headingRadians()
                    + (TowerLifeArt.hash(seed + generation, i + 11L) - 0.5) * 2 * s.halfWidthRadians() * 0.8;
            double span = s.reach() - s.inner();
            switch (fire) {
                case FLAME -> {
                    double r = s.inner() + span * (0.4 + 0.8 * age);
                    double rad = scale * 0.026 * (1 - 0.5 * age);
                    g2.setColor(TowerLifeArt.faded(mix(new Color(0xfff0a0), new Color(0xff6a20), age),
                            (1 - age) * Math.min(1, age * 6)));
                    g2.fill(new Ellipse2D.Double(s.x() + Math.cos(a) * r - rad, s.y() + Math.sin(a) * r - rad, rad * 2,
                            rad * 2));
                }
                case DRAGON -> {
                    double r = s.reach() * (0.92 + 0.25 * age);
                    double rad = scale * (0.08 + 0.12 * age);
                    g2.setColor(TowerLifeArt.faded(SMOKE, 0.5 * (1 - age) * Math.min(1, age * 5)));
                    g2.fill(new Ellipse2D.Double(s.x() + Math.cos(a) * r - rad, s.y() + Math.sin(a) * r - rad, rad * 2,
                            rad * 2));
                }
                case SOUL -> {
                    double r = s.inner() + span * (0.3 + 0.75 * age);
                    double curl = Math.sin(age * 7 + i) * scale * 0.12;
                    double px = s.x() + Math.cos(a) * r - hy * curl;
                    double py = s.y() + Math.sin(a) * r + hx * curl - age * scale * 0.25;
                    double rad = scale * 0.045 * (1.2 - 0.6 * age);
                    TowerLifeArt.glow(g2, px, py, rad * 3, WISP_GLOW, 0.35 * (1 - age));
                    g2.setColor(TowerLifeArt.faded(WISP, 0.85 * (1 - age) * Math.min(1, age * 4)));
                    g2.fill(new Ellipse2D.Double(px - rad, py - rad * 1.6, rad * 2, rad * 3.2));
                }
            }
        }
    }

    /**
     * An enemy in the fire. The jet piles up against its front: a hot glow pools on the side facing the nozzle. Its
     * body blocks the fire: a dimmer wake trails behind it downstream. Its dark silhouette is ringed by a rim as hot,
     * and as thick, as the fire in it. Tongues of flame split on its front and lick round both sides; a few embers shed
     * off its flanks.
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
        for (int side = -1; side <= 1; side += 2) {
            for (int k = 0; k < TONGUES; k++) {
                Shape t = lick(x, y, toNozzle, size, side, k, seconds, seed, 1.0);
                g2.setPaint(new GradientPaint((float) frontX, (float) frontY,
                        TowerLifeArt.faded(mix(flame, hot, 0.65), 0.95), (float) (x + Math.cos(down) * size * 3),
                        (float) (y + Math.sin(down) * size * 3), TowerLifeArt.faded(mix(flame, hot, 0.3), 0)));
                g2.fill(t);
            }
        }
        g2.setColor(TowerLifeArt.faded(SILHOUETTE, SILHOUETTE_ALPHA));
        g2.fill(outline);
        float rimWidth = (float) (RIM_CELLS * scale * (1 + rimShare(c.heat())));
        g2.setStroke(new BasicStroke(rimWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(TowerLifeArt.faded(hot, 0.85));
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
