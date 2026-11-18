package td.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;

/**
 * The shots of the tower visual polish (docs/features/FEATURE-tower-visual-polish.md, "Shots"): the Sniper's bullets
 * and ricochets by round, the Headhunter's laser, the execution mark, Frenzy's embers, and the Burst's stone at its new
 * size. Every method paints in board pixels; {@code scale} is the pixels to a cell. Each is a pure function of its
 * progress or clock and a seed.
 */
final class PolishShots {

    static final double LIFT = PolishLooks.LIFT;

    /**
     * What a Sniper's bullet is, which is how it is drawn: its tail and head colours, how wide its streak is (cells),
     * and the glow round it (cells wide, alpha). A ricochet is drawn as the bullet that made it.
     */
    enum Round {
        /** A plain shot: the tower's green to a white head, thin, no glow. */
        PLAIN(new Color(0x7ccf6a), Color.WHITE, 0.028, 0, 0, null),
        /** A crit seen after it landed (the fading trace): white. */
        CRIT(Color.WHITE, Color.WHITE, 0.032, 0.07, 0.30, Color.WHITE),
        /** The Fourth Shot, a sure crit: bold, red round a white-hot core, a wide red glow. */
        FOURTH(new Color(0xff4a4a), Color.WHITE, 0.052, 0.20, 0.55, new Color(0xff5a5a)),
        /** Hollow Point: bold copper with a hot orange head, a warm glow. */
        HOLLOW(new Color(0xc0581e), new Color(0xffc27a), 0.052, 0.15, 0.45, new Color(0xff8a3a)),
        /** Silver Round: bold silver-blue round a white core, a cold glow and glitter left behind. */
        SILVER(new Color(0x8fb8ff), new Color(0xf4f8ff), 0.052, 0.16, 0.50, new Color(0xb8d4ff));

        final Color tail;
        final Color head;
        final double width;
        final double glow;
        final double glowAlpha;
        final Color halo;

        Round(Color tail, Color head, double width, double glow, double glowAlpha, Color halo) {
            this.tail = tail;
            this.head = head;
            this.width = width;
            this.glow = glow;
            this.glowAlpha = glowAlpha;
            this.halo = halo;
        }
    }

    /** A bullet's streak is this share of its way, as the game's. */
    static final double STREAK_SHARE = 0.45;
    static final int STREAK_PIECES = 6;
    /** The Burst's thrown stone: half the size every lobbed shot had (the CannonballDraw size it is sent with). */
    static final float STONE_SIZE = 0.5f;
    /** The Headhunter's laser: how wide (cells) and how strong, and how fast it shimmers (Hz). */
    static final double LASER_CELLS = 0.010;
    static final double LASER_ALPHA = 0.75;
    static final double LASER_SHIMMER_HZ = 7;
    static final Color LASER = new Color(255, 60, 60);
    /** Frenzy's embers: how many rise at once, how long each lives (seconds, tick clock), how high (cells). */
    static final int FRENZY_EMBERS = 7;
    static final double FRENZY_EMBER_SECONDS = 0.9;
    static final double FRENZY_EMBER_RISE = 1.0;
    static final Color FRENZY_RED = new Color(0xff3a2a);

    private PolishShots() {
    }

    private static Color faded(Color color, double alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(),
                (int) Math.round(Math.clamp(alpha, 0, 1) * 255));
    }

    private static Color mixed(Color a, Color b, double share) {
        return new Color((int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * share),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * share),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * share));
    }

    private static void glow(Graphics2D g2, double x, double y, double radius, Color color, double alpha) {
        if (radius <= 0 || alpha <= 0) {
            return;
        }
        g2.setPaint(new RadialGradientPaint((float) x, (float) y, (float) radius, new float[] {0f, 1f},
                new Color[] {faded(color, alpha), faded(color, 0)}));
        g2.fill(new Ellipse2D.Double(x - radius, y - radius, radius * 2, radius * 2));
    }

    /**
     * A bullet of {@code round} {@code flown} of the way from {@code fromX, fromY} (the muzzle) to {@code toX, toY}:
     * its streak trails {@link #STREAK_SHARE} of the way behind its head. {@code alpha} fades a trace that has landed.
     */
    static void shot(Graphics2D g2, Round round, double fromX, double fromY, double toX, double toY, double flown,
                     double alpha, double scale, double seconds) {
        double tail = Math.max(0, flown - STREAK_SHARE);
        streak(g2, round, t -> new double[] {fromX + (toX - fromX) * t, fromY + (toY - fromY) * t}, tail, flown,
                alpha, scale, seconds);
    }

    /**
     * A ricochet of {@code round} {@code flown} of the way along its bowed arc from {@code fromX, fromY} to
     * {@code toX, toY} (the game's arc, bowed {@code bow} of the hop's length to one side): drawn as the bullet that made
     * it, so a Silver Round hops silver and a crit hops white.
     */
    static void ricochet(Graphics2D g2, Round round, double fromX, double fromY, double toX, double toY, double bow,
                         double flown, double scale, double seconds) {
        double bowX = (fromX + toX) / 2 - (toY - fromY) * bow;
        double bowY = (fromY + toY) / 2 + (toX - fromX) * bow;
        streak(g2, round, t -> new double[] {bezier(fromX, bowX, toX, t), bezier(fromY, bowY, toY, t)},
                Math.max(0, flown - STREAK_SHARE), flown, 1, scale, seconds);
    }

    private static double bezier(double from, double bow, double to, double t) {
        double rest = 1 - t;
        return rest * rest * from + 2 * rest * t * bow + t * t * to;
    }

    private interface Path {
        double[] at(double t);
    }

    /**
     * The streak from {@code tail} to {@code head} along {@code path}: its glow, then pieces each wider, brighter and
     * nearer the head colour than the one behind, then a white core on a bold round, then its head's glow. A Silver
     * Round leaves glitter behind.
     */
    private static void streak(Graphics2D g2, Round round, Path path, double tail, double head, double alpha,
                               double scale, double seconds) {
        if (head <= tail) {
            return;
        }
        double[][] p = new double[STREAK_PIECES + 1][];
        for (int i = 0; i <= STREAK_PIECES; i++) {
            p[i] = path.at(tail + (head - tail) * i / STREAK_PIECES);
        }
        if (round.glow > 0) {
            for (int i = 0; i < STREAK_PIECES; i++) {
                double share = (i + 1.0) / STREAK_PIECES;
                g2.setStroke(new BasicStroke((float) (round.glow * scale * (0.5 + 0.5 * share)), BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND));
                g2.setColor(faded(round.halo, round.glowAlpha * alpha * share * 0.5));
                g2.draw(new Line2D.Double(p[i][0], p[i][1], p[i + 1][0], p[i + 1][1]));
            }
        }
        for (int i = 0; i < STREAK_PIECES; i++) {
            double share = i / (STREAK_PIECES - 1.0);
            g2.setStroke(new BasicStroke((float) (round.width * scale * (0.55 + 0.45 * share)), BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
            g2.setColor(faded(mixed(round.tail, round.head, share * share), alpha * (0.3 + 0.7 * share)));
            g2.draw(new Line2D.Double(p[i][0], p[i][1], p[i + 1][0], p[i + 1][1]));
        }
        if (round.width > 0.04) {
            g2.setStroke(new BasicStroke((float) (round.width * scale * 0.38), BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
            g2.setColor(faded(round.head, alpha * 0.9));
            g2.draw(new Line2D.Double(p[STREAK_PIECES / 2][0], p[STREAK_PIECES / 2][1], p[STREAK_PIECES][0],
                    p[STREAK_PIECES][1]));
        }
        double[] tip = p[STREAK_PIECES];
        if (round.glow > 0) {
            glow(g2, tip[0], tip[1], round.glow * scale * 0.9, round.halo, round.glowAlpha * alpha);
            glow(g2, tip[0], tip[1], round.width * scale * 1.4, Color.WHITE, 0.9 * alpha);
        }
        if (round == Round.SILVER) {
            for (int i = 0; i < 5; i++) {
                double t = tail + (head - tail) * ((i + 0.5 + 0.3 * Math.sin(seconds * 9 + i * 2.3)) / 5.5);
                double[] at = path.at(Math.clamp(t, 0, 1));
                double off = (PolishLife.hash(i, (long) (seconds * 12)) - 0.5) * round.glow * scale;
                double size = scale * 0.012 * (0.6 + 0.8 * PolishLife.hash(i + 9, (long) (seconds * 12)));
                g2.setColor(faded(Color.WHITE, 0.85 * alpha));
                g2.fill(new Ellipse2D.Double(at[0] + off - size, at[1] - off - size, size * 2, size * 2));
            }
        }
    }

    /**
     * The Headhunter's laser while it aims at an Elite or a Boss: a very thin red line from the scope's lens at
     * {@code fromX, fromY} (lifted, board pixels) to the enemy at {@code toX, toY}, a faint glow along it that
     * shimmers, and a red dot where it lands. Wall clock: it is a sight, not an event.
     */
    static void laser(Graphics2D g2, double fromX, double fromY, double toX, double toY, double scale,
                      double seconds) {
        double shimmer = 0.85 + 0.15 * Math.sin(2 * Math.PI * LASER_SHIMMER_HZ * seconds);
        g2.setStroke(new BasicStroke((float) (LASER_CELLS * scale * 4), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(faded(LASER, 0.14 * shimmer));
        g2.draw(new Line2D.Double(fromX, fromY, toX, toY));
        g2.setStroke(new BasicStroke((float) Math.max(0.6, LASER_CELLS * scale), BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));
        g2.setColor(faded(LASER, LASER_ALPHA * shimmer));
        g2.draw(new Line2D.Double(fromX, fromY, toX, toY));
        glow(g2, toX, toY, scale * 0.09, LASER, 0.7 * shimmer);
        double dot = scale * 0.018;
        g2.setColor(faded(new Color(0xffd0c8), 0.95));
        g2.fill(new Ellipse2D.Double(toX - dot, toY - dot, dot * 2, dot * 2));
    }

    /**
     * Executioner's mark over an enemy the Sniper aims at that a crit would execute now (below an Elite and under the
     * threshold): a thin white ring with four ticks that slowly close on it. {@code sure} when the bullet on its way
     * is a sure crit as well: the ring is whole and bright, the ticks touch it, and a red point burns in its middle.
     */
    static void executionMark(Graphics2D g2, double x, double y, double scale, boolean sure, double seconds) {
        double breathe = 0.5 + 0.5 * Math.sin(seconds * 4.2);
        double r = scale * (sure ? 0.22 : 0.26 - 0.02 * breathe);
        double alpha = sure ? 0.95 : 0.55 + 0.25 * breathe;
        g2.setStroke(new BasicStroke((float) (scale * 0.016), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(faded(Color.WHITE, alpha));
        g2.draw(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
        double gap = sure ? 0 : scale * 0.05 * (1 - breathe * 0.5);
        for (int i = 0; i < 4; i++) {
            double a = Math.PI / 4 + i * Math.PI / 2;
            double from = r + gap;
            double to = from + scale * 0.09;
            g2.draw(new Line2D.Double(x + Math.cos(a) * from, y + Math.sin(a) * from, x + Math.cos(a) * to,
                    y + Math.sin(a) * to));
        }
        if (sure) {
            glow(g2, x, y, scale * 0.10, new Color(0xff4040), 0.8);
            double dot = scale * 0.022;
            g2.setColor(Color.WHITE);
            g2.fill(new Ellipse2D.Double(x - dot, y - dot, dot * 2, dot * 2));
        }
    }

    /**
     * Frenzy's embers: red sparks that leave the turret at {@code x, y} (its top, lifted, board pixels), one after
     * another, rise, waver and go out. {@code seconds} is the tick clock (they stop on pause, as Frenzy does).
     */
    static void frenzy(Graphics2D g2, double x, double y, double scale, double seconds, int seed) {
        for (int i = 0; i < FRENZY_EMBERS; i++) {
            double t = seconds / FRENZY_EMBER_SECONDS + (double) i / FRENZY_EMBERS + PolishLife.hash(seed, i) * 0.3;
            long life = (long) Math.floor(t);
            double age = t - life;
            double bearing = PolishLife.hash(life * 13 + i, seed) * 2 * Math.PI;
            double out = scale * 0.16 * (0.4 + 0.6 * PolishLife.hash(life * 7 + i, seed + 3L));
            double px = x + Math.cos(bearing) * out + Math.sin(age * 7 + i) * scale * 0.025;
            double py = y + Math.sin(bearing) * out * 0.5 - LIFT * FRENZY_EMBER_RISE * age * scale;
            double alpha = Math.min(1, age * 5) * (1 - age);
            double size = scale * (0.036 - 0.014 * age);
            glow(g2, px, py, size * 3.0, FRENZY_RED, 0.75 * alpha);
            g2.setColor(faded(mixed(new Color(0xffe0a0), FRENZY_RED, Math.min(1, age * 1.6)), alpha));
            Path2D.Double spark = new Path2D.Double();
            spark.moveTo(px, py - size * 1.6);
            spark.lineTo(px + size * 0.7, py);
            spark.lineTo(px, py + size * 1.1);
            spark.lineTo(px - size * 0.7, py);
            spark.closePath();
            g2.fill(spark);
        }
    }
}
