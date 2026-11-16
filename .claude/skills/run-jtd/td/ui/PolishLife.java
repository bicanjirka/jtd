package td.ui;

import td.ui.render.LoadedMissile;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * Everything of the tower visual polish that is painted live (docs/features/FEATURE-tower-visual-polish.md): idle
 * life on the wall clock and moments on the tick clock. Every method paints in board pixels, as the game's painters
 * do: {@code scale} is the pixels to a cell, a position is where the thing stands on the board. Each is a pure
 * function of its clock or progress and a seed, so nothing is stored between frames.
 */
final class PolishLife {

    static final double LIFT = PolishLooks.LIFT;

    // ---- idle: the lives of PolishLooks.Life -------------------------------------------------------------------------

    private static final double BREATH_SECONDS = 3.4;
    private static final double GLINT_SECONDS = 5.5;
    private static final double GLINT_SHOWS_SECONDS = 0.45;

    /** An arc jumps about this often from each of a ball's two sides, and shows for this long. */
    static final double SPARK_SECONDS = 1.15;
    static final double SPARK_SHOWS_SECONDS = 0.28;
    /** An arc is redrawn this many times a second, which is what makes it crackle. */
    static final double SPARK_JITTER_HZ = 24;
    static final int SPARK_JOINTS = 5;
    static final double SPARK_STROKE_CELLS = 0.028;

    /** How fast each mote turns inside the ball, in radians a second, and how far out it rides. */
    static final double[] SWIRL_SPEED = {1.3, -0.9, 0.6};
    static final double[] SWIRL_REACH = {0.28, 0.52, 0.70};

    /** A wave of light climbs the runes once in this long; this many radians of it fit in a cell of screen height. */
    static final double RUNE_SECONDS = 4.2;
    static final double RUNE_WAVE = 9.0;

    // ---- idle: the nest's orbit and the birds ------------------------------------------------------------------------

    /** How far a level circle is flattened on screen. 1 is the board's own projection; see the doc's "Perspective". */
    static final double ORBIT_SQUASH = 0.62;
    /** The nest's ring: its radius in cells and how fast it turns while the tower idles, in radians a second. */
    static final double NEST_RADIUS_CELLS = 0.27;
    static final double NEST_TURN = 0.6;

    /** The birds of one Aerie look. */
    enum Flock {
        /** The plain Aerie: snow terns, quick and pale. */
        TERNS(new Color(0xe4f6fc), 0.14, 3.2, 0.85, 1, false, 0),
        /** Ruby: ember birds, small and fast, each trailing a glow. */
        EMBERS(new Color(0xff9a6a), 0.10, 5.0, 1.35, 1, false, 0.9),
        /** Shade: crows, ragged, circling the other way in a loose crowd. */
        CROWS(new Color(0x8f9aa8), 0.15, 2.4, 0.7, -1, true, 0),
        /** The White tower: great eagles that glide and seldom beat their wings. */
        EAGLES(new Color(0xfff3d0), 0.22, 1.1, 0.45, 1, false, 0);

        final Color color;
        final double span;
        final double flapHz;
        final double speed;
        final int turn;
        final boolean ragged;
        final double trail;

        Flock(Color color, double span, double flapHz, double speed, int turn, boolean ragged, double trail) {
            this.color = color;
            this.span = span;
            this.flapHz = flapHz;
            this.speed = speed;
            this.turn = turn;
            this.ragged = ragged;
            this.trail = trail;
        }

        /** How many circle a tower of {@code tier}. */
        int count(int tier) {
            int base = this == EAGLES ? 1 : this == CROWS ? 3 : 2;
            return base + (tier >= 2 ? 1 : 0) + (tier >= 3 ? 1 : 0);
        }
    }

    // ---- moments -----------------------------------------------------------------------------------------------------

    /** A spent cartridge flies for this long, in seconds of game time. */
    static final double CASING_SECONDS = 0.45;
    static final Color CASING_BRASS = new Color(0xe8c060);
    /** A Scorcher lets off steam for this long once its reload starts. */
    static final double STEAM_SECONDS = 0.9;
    static final int STEAM_PUFFS = 5;
    static final Color STEAM = new Color(0xdfe6ee);
    /** The Sundial's shadow: a dark body that dims what it lies on, a veil that shows on a black floor, a lit edge. */
    static final Color SUNDIAL_DARK = new Color(0x06040e);
    static final float SUNDIAL_DARK_ALPHA = 0.78f;
    static final float SUNDIAL_VEIL_ALPHA = 0.34f;
    static final float SUNDIAL_TIP_ALPHA = 0.10f;
    static final float SUNDIAL_EDGE_ALPHA = 0.75f;
    static final double SUNDIAL_EDGE_CELLS = 0.028;

    private PolishLife() {
    }

    /** A number in 0..1 that depends only on {@code a} and {@code b}. */
    static double hash(long a, long b) {
        long h = a * 0x9E3779B97F4A7C15L ^ b * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return (h & 0xFFFFFF) / (double) 0x1000000;
    }

    private static Color faded(Color color, double alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(),
                (int) Math.round(Math.clamp(alpha, 0, 1) * 255));
    }

    private static void glow(Graphics2D g2, double x, double y, double radius, Color color, double alpha) {
        if (radius <= 0 || alpha <= 0) {
            return;
        }
        g2.setPaint(new RadialGradientPaint((float) x, (float) y, (float) radius, new float[] {0f, 1f},
                new Color[] {faded(color, alpha), faded(color, 0)}));
        g2.fill(new Ellipse2D.Double(x - radius, y - radius, radius * 2, radius * 2));
    }

    /** Every live light of {@code tower} standing at {@code cx, cy}, its head turned to {@code heading}. */
    static void paintLives(Graphics2D g2, PolishLooks.Built tower, double cx, double cy, double scale, double heading,
                           double seconds, int seed) {
        double cos = Math.cos(heading);
        double sin = Math.sin(heading);
        for (PolishLooks.Live light : tower.lives()) {
            double lx = light.turning() ? light.x() * cos - light.y() * sin : light.x();
            double ly = light.turning() ? light.x() * sin + light.y() * cos : light.y();
            double x = cx + lx * scale;
            double y = cy + (ly - LIFT * light.z()) * scale;
            double radius = light.radius() * scale;
            switch (light.life()) {
                case BREATH -> {
                    double breath = Math.sin(2 * Math.PI * seconds / BREATH_SECONDS);
                    glow(g2, x, y, radius * (1 + 0.16 * breath), light.color(), 0.34 + 0.26 * breath);
                }
                case FLICKER -> {
                    double flick = 0.5 * Math.sin(seconds * 23.0) + 0.3 * Math.sin(seconds * 37.7 + 1.3)
                            + 0.2 * Math.sin(seconds * 61.1 + 0.4);
                    glow(g2, x, y, radius * (1.15 + 0.45 * flick), light.color(), 0.5 + 0.3 * flick);
                }
                case GLINT -> {
                    double since = seconds % GLINT_SECONDS;
                    if (since < GLINT_SHOWS_SECONDS) {
                        double shine = Math.sin(Math.PI * since / GLINT_SHOWS_SECONDS);
                        g2.setColor(faded(light.color(), shine));
                        g2.fill(glint(x, y, radius * (0.4 + 0.6 * shine)));
                    }
                }
                case SPARKS -> sparks(g2, x, y, radius, scale, light.color(), seconds, seed);
                case SWIRL -> swirl(g2, x, y, radius, light.color(), seconds, seed);
                case RUNE -> rune(g2, x, y, radius, light.color(), seconds, (cy - y) / scale);
            }
        }
    }

    private static Shape glint(double x, double y, double size) {
        double waist = size * 0.16;
        Path2D.Double star = new Path2D.Double();
        star.moveTo(x, y - size);
        star.lineTo(x + waist, y - waist);
        star.lineTo(x + size, y);
        star.lineTo(x + waist, y + waist);
        star.lineTo(x, y + size);
        star.lineTo(x - waist, y + waist);
        star.lineTo(x - size, y);
        star.lineTo(x - waist, y - waist);
        star.closePath();
        return star;
    }

    /**
     * Life.SPARKS: two arcs, each jumping from the ball at {@code x, y} once in {@link #SPARK_SECONDS} toward a new
     * bearing, up to {@code reach} pixels long and drooping toward the coil. While one shows it is redrawn with new
     * joints {@link #SPARK_JITTER_HZ} times a second and fades; a fork leaves its middle.
     */
    static void sparks(Graphics2D g2, double x, double y, double reach, double scale, Color color, double seconds,
                       int seed) {
        long frame = (long) Math.floor(seconds * SPARK_JITTER_HZ);
        for (int side = 0; side < 3; side++) {
            double t = seconds / SPARK_SECONDS + hash(seed, side) + side / 3.0;
            long jump = (long) Math.floor(t);
            double since = (t - jump) * SPARK_SECONDS;
            if (since >= SPARK_SHOWS_SECONDS) {
                continue;
            }
            double fade = 1 - since / SPARK_SHOWS_SECONDS;
            double bearing = hash(jump * 31 + side, seed) * 2 * Math.PI;
            double length = reach * (0.6 + 0.4 * hash(jump * 17 + side, seed + 1L));
            double droop = length * (0.3 + 0.9 * hash(jump * 13 + side, seed + 2L)) * LIFT;
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
            g2.setStroke(new BasicStroke((float) (scale * SPARK_STROKE_CELLS * 2.2), BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
            g2.setColor(faded(new Color(0x8496ff), 0.35 * fade));
            g2.draw(arc);
            g2.setStroke(new BasicStroke((float) (scale * SPARK_STROKE_CELLS), BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
            g2.setColor(faded(color, 0.95 * fade));
            g2.draw(arc);
            g2.setStroke(new BasicStroke((float) (scale * SPARK_STROKE_CELLS * 0.7), BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
            g2.draw(fork);
        }
    }

    /**
     * Life.SWIRL: the magic inside a glass ball centred at {@code x, y}, clipped to {@code radius}: three soft motes
     * turning at their own speeds, two wisps of light sweeping against each other, and the glass's glint over them.
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
        g2.setStroke(new BasicStroke((float) (radius * 0.16), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
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
     * Life.RUNE: one rune's glow at {@code x, y}; {@code climb} is how far up the screen it stands above the cell's
     * centre, in cells. The runes of one stone light one after another from the foot up, then rest.
     */
    static void rune(Graphics2D g2, double x, double y, double radius, Color color, double seconds, double climb) {
        double wave = Math.sin(2 * Math.PI * seconds / RUNE_SECONDS - climb * RUNE_WAVE);
        double lit = Math.max(0, wave);
        glow(g2, x, y, radius * (0.8 + 0.5 * lit), color, 0.85 * lit * lit);
        glow(g2, x, y, radius * 0.45, Color.WHITE, 0.55 * lit * lit * lit);
    }

    /**
     * The nest: {@code missiles} evenly spaced on a ring round {@code cx, cy} (the tower's centre lifted to
     * {@code Foundation.nest}), turning with {@code phase}. An idle tower passes {@code seconds * NEST_TURN}; the
     * tower's own ring phase is added to it. The ring is a level circle, flattened by {@code squash} on screen.
     */
    static void nest(Graphics2D g2, List<LoadedMissile> missiles, double cx, double cy, double scale, double squash,
                     double phase, boolean ready) {
        AffineTransform save = g2.getTransform();
        double r = NEST_RADIUS_CELLS * scale;
        int count = missiles.size();
        for (int i = 0; i < count; i++) {
            double a = phase + Math.PI * 2 * i / count;
            g2.translate(cx + Math.cos(a) * r, cy + Math.sin(a) * r * squash);
            g2.rotate(Math.atan2(Math.cos(a) * squash, -Math.sin(a)));
            MissileArt.paint(g2, missiles.get(i), (float) (0.22 * scale * (0.82 + 0.18 * Math.sin(a))), ready);
            g2.setTransform(save);
        }
    }

    /**
     * The birds of {@code flock} circling {@code cx, cy}: the tower's centre lifted to its top. Each keeps its own
     * ring, height, speed and wing beat, all from {@code seed}, so two towers never fly alike.
     */
    static void birds(Graphics2D g2, Flock flock, int tier, double cx, double cy, double scale, double squash,
                      double seconds, int seed) {
        for (int i = 0; i < flock.count(tier); i++) {
            double h1 = hash(seed, i);
            double h2 = hash(seed + 7L, i);
            double h3 = hash(seed + 13L, i);
            double ring = (0.36 + 0.16 * h1) * scale;
            int turn = flock.ragged && i % 3 == 2 ? -flock.turn : flock.turn;
            double a = turn * seconds * flock.speed * (0.8 + 0.4 * h2) + h3 * 2 * Math.PI;
            double rise = 0.10 + 0.24 * h2 + 0.03 * Math.sin(seconds * 0.9 + i * 2.1);
            double x = cx + Math.cos(a) * ring;
            double y = cy + Math.sin(a) * ring * squash - LIFT * rise * scale;
            double heading = Math.atan2(turn * Math.cos(a) * squash, -turn * Math.sin(a));
            double beat = Math.sin(2 * Math.PI * flock.flapHz * seconds + h1 * 6.28);
            if (flock == Flock.EAGLES && Math.sin(seconds * 0.8 + i * 1.9) < 0.55) {
                beat = 0.25;
            }
            double near = 0.85 + 0.15 * Math.sin(a);
            bird(g2, flock, x, y, heading, beat, flock.span * scale * near);
        }
    }

    private static void bird(Graphics2D g2, Flock flock, double x, double y, double heading, double beat,
                             double span) {
        AffineTransform save = g2.getTransform();
        g2.translate(x, y);
        g2.rotate(heading);
        if (flock.trail > 0) {
            glow(g2, -span * 0.5, 0, span * flock.trail, flock.color, 0.5);
        }
        double reach = span / 2 * (0.62 + 0.38 * Math.abs(beat));
        double sweep = span * (-0.16 + 0.14 * beat);
        g2.setColor(flock.color);
        for (int side = -1; side <= 1; side += 2) {
            Path2D.Double wing = new Path2D.Double();
            wing.moveTo(span * 0.14, 0);
            wing.quadTo(span * 0.16, side * reach * 0.6, sweep, side * reach);
            if (flock.ragged) {
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

    /**
     * A spent cartridge thrown from a rifle's breech, {@code progress} of the way through {@link #CASING_SECONDS} on
     * the tick clock: out to the rifle's right, up and over, spinning, down to the ground, gone. {@code cx, cy} is the
     * tower's cell centre and {@code breech} how high the rifle stands.
     */
    static void casing(Graphics2D g2, double cx, double cy, double scale, double heading, double breech,
                       double progress) {
        if (progress < 0 || progress >= 1) {
            return;
        }
        double side = heading + Math.PI / 2;
        double out = 0.30 * (1 - (1 - progress) * (1 - progress));
        double high = breech + 0.9 * progress - (breech + 0.9) * progress * progress;
        double x = cx + (Math.cos(side) * out - Math.cos(heading) * 0.06 * progress) * scale;
        double groundY = cy + (Math.sin(side) * out - Math.sin(heading) * 0.06 * progress) * scale;
        double alpha = progress < 0.8 ? 1 : (1 - progress) / 0.2;
        g2.setColor(faded(Color.BLACK, 0.35 * alpha));
        g2.fill(new Ellipse2D.Double(x - scale * 0.025, groundY - scale * 0.012, scale * 0.05, scale * 0.024));
        AffineTransform save = g2.getTransform();
        g2.translate(x, groundY - LIFT * high * scale);
        g2.rotate(progress * 2.5 * 2 * Math.PI);
        g2.setColor(faded(CASING_BRASS, alpha));
        g2.fill(new Rectangle2D.Double(-scale * 0.032, -scale * 0.013, scale * 0.064, scale * 0.026));
        g2.setColor(faded(Color.WHITE, 0.7 * alpha));
        g2.fill(new Rectangle2D.Double(-scale * 0.032, -scale * 0.013, scale * 0.064, scale * 0.009));
        g2.setTransform(save);
    }

    /**
     * Steam let off at {@code x, y} (the nozzle's tip, already lifted), {@code progress} of the way through
     * {@link #STEAM_SECONDS} on the tick clock from the moment the reload starts: puffs that leave one after another,
     * rise, drift, swell and thin out.
     */
    static void steam(Graphics2D g2, double x, double y, double scale, double progress, int seed) {
        if (progress < 0 || progress >= 1) {
            return;
        }
        for (int i = 0; i < STEAM_PUFFS; i++) {
            double age = (progress - i * 0.11) / 0.55;
            if (age <= 0 || age >= 1) {
                continue;
            }
            double px = x + scale * (0.035 * Math.sin(i * 2.4 + age * 3) + (hash(seed, i) - 0.5) * 0.06);
            double py = y - scale * (0.03 + 0.30 * age);
            double alpha = 0.62 * (1 - age) * Math.min(1, age * 6);
            glow(g2, px, py, scale * (0.05 + 0.085 * age), STEAM, alpha);
            glow(g2, px, py, scale * (0.025 + 0.04 * age), Color.WHITE, alpha * 0.6);
        }
    }

    /**
     * The Sundial as the stone's own shadow: the menhir's outline laid on the ground from the stone's foot at
     * {@code cx, cy} along {@code heading}, {@code reach} pixels long. A dark body dims what it crosses, a violet veil
     * keeps it visible on the black floor, and the leading edge, where the hit lands, is lit. {@code alpha} fades it
     * in and out as the wedge's did; {@code umbra} is the darker, bluer veil of an Umbra stone inside its field.
     */
    static void sundial(Graphics2D g2, double cx, double cy, double scale, double heading, int tier, double reach,
                        double alpha, boolean umbra, Color veil) {
        AffineTransform save = g2.getTransform();
        g2.translate(cx, cy);
        g2.rotate(heading);
        Shape shadow = AffineTransform.getScaleInstance(scale, scale)
                .createTransformedShape(PolishLooks.menhirShadow(tier, reach / scale));
        float end = (float) Math.max(1, reach);
        g2.setPaint(new LinearGradientPaint(0f, 0f, end, 0f, new float[] {0f, 1f}, new Color[] {
                faded(SUNDIAL_DARK, SUNDIAL_DARK_ALPHA * alpha), faded(SUNDIAL_DARK, SUNDIAL_DARK_ALPHA * 0.45 * alpha)}));
        g2.fill(shadow);
        Color tint = umbra ? new Color(0x6a58b0) : veil;
        g2.setPaint(new LinearGradientPaint(0f, 0f, end, 0f, new float[] {0f, 1f}, new Color[] {
                faded(tint, (umbra ? 0.42 : SUNDIAL_VEIL_ALPHA) * alpha), faded(tint, SUNDIAL_TIP_ALPHA * alpha)}));
        g2.fill(shadow);
        Color edge = new Color((tint.getRed() + 510) / 3, (tint.getGreen() + 510) / 3, (tint.getBlue() + 510) / 3);
        g2.setPaint(new LinearGradientPaint(0f, 0f, end, 0f, new float[] {0f, 1f}, new Color[] {
                faded(edge, SUNDIAL_EDGE_ALPHA * alpha), faded(edge, SUNDIAL_EDGE_ALPHA * 0.35 * alpha)}));
        g2.setStroke(new BasicStroke((float) (SUNDIAL_EDGE_CELLS * scale), BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));
        Shape clip = g2.getClip();
        g2.clip(new Rectangle2D.Double(scale * 0.02, -scale * 2, reach + scale, scale * 2));
        g2.draw(shadow);
        g2.setClip(clip);
        g2.setTransform(save);
    }
}
