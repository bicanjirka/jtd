package td.ui;

import td.ui.render.FlockDraw;
import td.ui.render.FlockLook;
import td.ui.render.NestDraw;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

/**
 * What keeps a tower alive while it stands there, painted live in board pixels: the sparks off a coil's ball, the magic
 * turning in a glass ball, a wave of light up a stone's runes, the nest's missiles circling an Aerie's top and the birds
 * over a quiet one. Each is a pure function of the wall clock (and, for the birds' coming and going, the tick clock)
 * and a seed, so nothing is kept between frames.
 */
final class TowerLifeArt {

    /** An arc jumps about this often from each of a ball's sides, and shows for this long. */
    private static final double SPARK_SECONDS = 1.15;
    private static final double SPARK_SHOWS_SECONDS = 0.28;
    private static final int SPARK_SIDES = 3;
    /** An arc is redrawn this many times a second, which is what makes it crackle. */
    private static final double SPARK_JITTER_HZ = 24;
    private static final int SPARK_JOINTS = 5;
    private static final double SPARK_STROKE_CELLS = 0.028;
    private static final Color SPARK_HALO = new Color(0x8496ff);

    /** How fast each mote turns inside the ball, in radians a second, and how far out it rides. */
    private static final double[] SWIRL_SPEED = {1.3, -0.9, 0.6};
    private static final double[] SWIRL_REACH = {0.28, 0.52, 0.70};

    /** A wave of light climbs the runes once in this long; this many radians of it fit in a cell of screen height. */
    private static final double RUNE_SECONDS = 4.2;
    private static final double RUNE_WAVE = 9.0;

    /**
     * How far a level circle the nest and the birds fly on is flattened on screen, so they read as circling a spire; the
     * board's own projection would leave it round.
     */
    private static final double ORBIT_SQUASH = 0.62;
    /** A missile on the near side of the ring is drawn up to this much longer than one on the far side. */
    private static final double NEST_NEAR_GROWTH = 0.18;

    /** Birds come in from this many rings out and this many cells higher, and flee to these. */
    private static final double BIRD_COME_FROM = 5.0;
    private static final double BIRD_COME_HIGH = 1.2;
    private static final double BIRD_FLEE_TO = 7.0;
    private static final double BIRD_FLEE_HIGH = 1.8;
    /** How far a bird's ring breathes in and out as it circles, a share of the ring. */
    private static final double BIRD_WANDER = 0.10;

    private TowerLifeArt() {
    }

    /** How many birds of {@code look} circle a tower of {@code tier}: more as it grows. */
    static int birdCount(FlockLook look, int tier) {
        int base = switch (look) {
            case EAGLES -> 1;
            case CROWS -> 3;
            case TERNS, EMBERS -> 2;
        };
        return base + (tier >= 2 ? 1 : 0) + (tier >= 3 ? 1 : 0);
    }

    /** How a flock looks and flies. */
    private enum Flock {
        TERNS(new Color(0xe4f6fc), 0.14, 3.2, 0.85, 1, false, 0, false),
        EMBERS(new Color(0xff9a6a), 0.10, 5.0, 1.35, 1, false, 0.9, false),
        CROWS(new Color(0x8f9aa8), 0.15, 2.4, 0.7, -1, true, 0, false),
        EAGLES(new Color(0xfff3d0), 0.22, 1.1, 0.45, 1, false, 0, true);

        private final Color color;
        private final double span;
        private final double flapHz;
        private final double speed;
        private final int turn;
        private final boolean ragged;
        private final double trail;
        private final boolean glides;

        Flock(Color color, double span, double flapHz, double speed, int turn, boolean ragged, double trail,
              boolean glides) {
            this.color = color;
            this.span = span;
            this.flapHz = flapHz;
            this.speed = speed;
            this.turn = turn;
            this.ragged = ragged;
            this.trail = trail;
            this.glides = glides;
        }

        Color color() {
            return this.color;
        }

        double span() {
            return this.span;
        }

        double flapHz() {
            return this.flapHz;
        }

        double speed() {
            return this.speed;
        }

        int turn() {
            return this.turn;
        }

        boolean ragged() {
            return this.ragged;
        }

        double trail() {
            return this.trail;
        }

        boolean glides() {
            return this.glides;
        }
    }

    private static Flock flockOf(FlockLook look) {
        return switch (look) {
            case TERNS -> Flock.TERNS;
            case EMBERS -> Flock.EMBERS;
            case CROWS -> Flock.CROWS;
            case EAGLES -> Flock.EAGLES;
        };
    }

    /** A number in 0..1 that depends only on {@code a} and {@code b}. */
    static double hash(long a, long b) {
        long h = a * 0x9E3779B97F4A7C15L ^ b * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return (h & 0xFFFFFF) / (double) 0x1000000;
    }

    static Color faded(Color color, double alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(),
                (int) Math.round(Math.clamp(alpha, 0, 1) * 255));
    }

    static void glow(Graphics2D g2, double x, double y, double radius, Color color, double alpha) {
        if (radius <= 0 || alpha <= 0) {
            return;
        }
        g2.setPaint(new RadialGradientPaint((float) x, (float) y, (float) radius, new float[] {0f, 1f},
                new Color[] {faded(color, alpha), faded(color, 0)}));
        g2.fill(new Ellipse2D.Double(x - radius, y - radius, radius * 2, radius * 2));
    }

    private static BasicStroke round(double width) {
        return new BasicStroke((float) width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }

    /**
     * Arcs jumping from the ball at {@code x, y}: each side's leaps once in a while toward a new bearing, up to
     * {@code reach} pixels long and drooping toward the coil; while one shows it is redrawn with new joints many times a
     * second and fades, and a fork leaves its middle.
     */
    static void sparks(Graphics2D g2, double x, double y, double reach, double scale, Color color, double seconds,
                       int seed) {
        long frame = (long) Math.floor(seconds * SPARK_JITTER_HZ);
        for (int side = 0; side < SPARK_SIDES; side++) {
            double t = seconds / SPARK_SECONDS + hash(seed, side) + side / (double) SPARK_SIDES;
            long jump = (long) Math.floor(t);
            double since = (t - jump) * SPARK_SECONDS;
            if (since >= SPARK_SHOWS_SECONDS) {
                continue;
            }
            double fade = 1 - since / SPARK_SHOWS_SECONDS;
            double bearing = hash(jump * 31 + side, seed) * 2 * Math.PI;
            double length = reach * (0.6 + 0.4 * hash(jump * 17 + side, seed + 1L));
            double droop = length * (0.3 + 0.9 * hash(jump * 13 + side, seed + 2L)) * TowerFoundations.LIFT;
            Path2D.Double arc = new Path2D.Double();
            Path2D.Double fork = new Path2D.Double();
            for (int i = 0; i <= SPARK_JOINTS; i++) {
                double u = (double) i / SPARK_JOINTS;
                double off = i == 0 || i == SPARK_JOINTS ? 0
                        : (hash(jump * 7 + i, frame * 3 + side) - 0.5) * length * 0.34;
                double px = x + Math.cos(bearing) * length * u - Math.sin(bearing) * off;
                double py = y + Math.sin(bearing) * length * u * 0.8 + Math.cos(bearing) * off + droop * u * u;
                if (i == 0) {
                    arc.moveTo(px, py);
                } else {
                    arc.lineTo(px, py);
                }
                if (i == 3) {
                    double turn = bearing + (hash(jump, frame + side) - 0.5) * 2.4;
                    fork.moveTo(px, py);
                    fork.lineTo(px + Math.cos(turn) * length * 0.3, py + Math.sin(turn) * length * 0.3);
                }
            }
            glow(g2, x, y, scale * 0.16, color, 0.8 * fade);
            g2.setStroke(round(scale * SPARK_STROKE_CELLS * 2.2));
            g2.setColor(faded(SPARK_HALO, 0.35 * fade));
            g2.draw(arc);
            g2.setStroke(round(scale * SPARK_STROKE_CELLS));
            g2.setColor(faded(color, 0.95 * fade));
            g2.draw(arc);
            g2.setStroke(round(scale * SPARK_STROKE_CELLS * 0.7));
            g2.draw(fork);
        }
    }

    /**
     * The magic inside a glass ball centred at {@code x, y}, kept inside {@code radius}: soft motes turning at their own
     * speeds, two wisps of light sweeping against each other, and the glass's glint over them.
     */
    static void swirl(Graphics2D g2, double x, double y, double radius, Color color, double seconds, int seed) {
        Shape clip = g2.getClip();
        g2.clip(new Ellipse2D.Double(x - radius, y - radius, radius * 2, radius * 2));
        for (int i = 0; i < SWIRL_SPEED.length; i++) {
            double a = seconds * SWIRL_SPEED[i] + hash(seed, i) * 2 * Math.PI;
            double pulse = 0.75 + 0.25 * Math.sin(seconds * 1.7 + i * 2.1);
            glow(g2, x + Math.cos(a) * radius * SWIRL_REACH[i], y + Math.sin(a) * radius * SWIRL_REACH[i] * 0.85,
                    radius * 0.62 * pulse, color, 0.62);
        }
        g2.setStroke(round(radius * 0.16));
        g2.setColor(faded(Color.WHITE, 0.5));
        double sweep = Math.toDegrees(seconds * 0.9) + hash(seed, 9) * 360;
        g2.draw(new Arc2D.Double(x - radius * 0.62, y - radius * 0.62, radius * 1.24, radius * 1.24, sweep, 100,
                Arc2D.OPEN));
        g2.setColor(faded(color, 0.6));
        g2.draw(new Arc2D.Double(x - radius * 0.34, y - radius * 0.34, radius * 0.68, radius * 0.68, -sweep * 1.6, 130,
                Arc2D.OPEN));
        g2.setClip(clip);
        g2.setColor(faded(Color.WHITE, 0.8));
        g2.fill(new Ellipse2D.Double(x - radius * 0.62, y - radius * 0.66, radius * 0.36, radius * 0.30));
    }

    /**
     * One rune's glow at {@code x, y}; {@code climb} is how far up the screen it stands above the cell's centre, in
     * cells, so the runes of one stone light one after another from the foot up, then rest.
     */
    static void rune(Graphics2D g2, double x, double y, double radius, Color color, double seconds, double climb) {
        double wave = Math.sin(2 * Math.PI * seconds / RUNE_SECONDS - climb * RUNE_WAVE);
        double lit = Math.max(0, wave);
        glow(g2, x, y, radius * (0.8 + 0.5 * lit), color, 0.85 * lit * lit);
        glow(g2, x, y, radius * 0.45, Color.WHITE, 0.55 * lit * lit * lit);
    }

    /**
     * The nest's missiles spread evenly on a flattened level ring round the tower's top, the first at the draw's phase,
     * each turned along the ring and drawn a little longer on its near side.
     */
    static void nest(Graphics2D g2, NestDraw nest) {
        AffineTransform save = g2.getTransform();
        float r = nest.ringRadius();
        int count = nest.missiles().size();
        for (int i = 0; i < count; i++) {
            double a = nest.phaseRadians() + Math.PI * 2 * i / count;
            g2.translate(nest.centerX() + Math.cos(a) * r, nest.centerY() + Math.sin(a) * r * ORBIT_SQUASH);
            g2.rotate(Math.atan2(Math.cos(a) * ORBIT_SQUASH, -Math.sin(a)));
            MissileArt.paint(g2, nest.missiles().get(i),
                    (float) (nest.missileLength() * (1 - NEST_NEAR_GROWTH + NEST_NEAR_GROWTH * Math.sin(a))),
                    nest.releaseReady());
            g2.setTransform(save);
        }
    }

    /**
     * The birds circling a quiet Aerie's top. Each keeps its own ring, height, speed and wing beat, all from the seed,
     * so two towers never fly alike; they come in one after another from far and high, and scatter, climbing and
     * beating faster, once the tower fights.
     */
    static void birds(Graphics2D g2, FlockDraw birds) {
        Flock flock = flockOf(birds.look());
        double scale = birds.cellPixels();
        double seconds = birds.seconds();
        int seed = birds.seed();
        FlockPresence presence = new FlockPresence(birds.arrived(), birds.leaving());
        int count = birds.count();
        for (int i = 0; i < count; i++) {
            double h1 = hash(seed, i);
            double h2 = hash(seed + 7L, i);
            double h3 = hash(seed + 13L, i);
            double in = presence.in(i, count);
            double out = presence.out(hash(seed + 29L, i));
            if (in <= 0 || out >= 1) {
                continue;
            }
            double far = 1 - Easing.inOut(in);
            double away = Math.pow(out, 1.6);
            double wander = 1 + BIRD_WANDER * Math.sin(seconds * (0.25 + 0.2 * h1) + h3 * 6.28);
            double ring = (0.36 + 0.16 * h1) * wander * (1 + BIRD_COME_FROM * far + BIRD_FLEE_TO * away) * scale;
            int turn = flock.ragged() && i % 3 == 2 ? -flock.turn() : flock.turn();
            double a = turn * seconds * flock.speed() * (0.8 + 0.4 * h2) + h3 * 2 * Math.PI;
            double rise = 0.10 + 0.24 * h2 + 0.03 * Math.sin(seconds * 0.9 + i * 2.1) + BIRD_COME_HIGH * far
                    + BIRD_FLEE_HIGH * Math.pow(out, 1.3);
            double x = birds.centerX() + Math.cos(a) * ring;
            double y = birds.centerY() + Math.sin(a) * ring * ORBIT_SQUASH - TowerFoundations.LIFT * rise * scale;
            double along = Math.atan2(turn * Math.cos(a) * ORBIT_SQUASH, -turn * Math.sin(a));
            double outward = Math.atan2(Math.sin(a) * ORBIT_SQUASH - TowerFoundations.LIFT * 0.6, Math.cos(a));
            double inward = outward + Math.PI;
            double heading = turnToward(turnToward(along, inward, 0.6 * far), outward, Math.min(1, out * 2.5));
            double beat = Math.sin(2 * Math.PI * flock.flapHz() * (1 + out * 1.5) * seconds + h1 * 6.28);
            if (flock.glides() && out == 0 && Math.sin(seconds * 0.8 + i * 1.9) < 0.55) {
                beat = 0.25;
            }
            double near = 0.85 + 0.15 * Math.sin(a);
            double alpha = Math.min(1, in * 4) * Math.min(1, (1 - out) * 3.5);
            bird(g2, flock, x, y, heading, beat, flock.span() * scale * near, alpha);
        }
    }

    /** {@code from} turned {@code share} of the short way toward {@code to}. */
    private static double turnToward(double from, double to, double share) {
        double d = Math.atan2(Math.sin(to - from), Math.cos(to - from));
        return from + d * share;
    }

    private static void bird(Graphics2D g2, Flock flock, double x, double y, double heading, double beat, double span,
                             double alpha) {
        AffineTransform save = g2.getTransform();
        g2.translate(x, y);
        g2.rotate(heading);
        if (flock.trail() > 0) {
            glow(g2, -span * 0.5, 0, span * flock.trail(), flock.color(), 0.5 * alpha);
        }
        double reach = span / 2 * (0.62 + 0.38 * Math.abs(beat));
        double sweep = span * (-0.16 + 0.14 * beat);
        g2.setColor(faded(flock.color(), alpha));
        for (int side = -1; side <= 1; side += 2) {
            Path2D.Double wing = new Path2D.Double();
            wing.moveTo(span * 0.14, 0);
            wing.quadTo(span * 0.16, side * reach * 0.6, sweep, side * reach);
            if (flock.ragged()) {
                wing.lineTo(sweep - span * 0.05, side * reach * 0.70);
                wing.lineTo(sweep - span * 0.16, side * reach * 0.62);
            }
            wing.lineTo(-span * 0.14, side * reach * 0.30);
            wing.lineTo(-span * 0.10, 0);
            wing.closePath();
            g2.fill(wing);
        }
        g2.fill(new Ellipse2D.Double(-span * 0.26, -span * 0.09, span * 0.52, span * 0.18));
        g2.setTransform(save);
    }
}
