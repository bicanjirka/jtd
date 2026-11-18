package td.ui;

import td.ui.render.BulletDraw;
import td.ui.render.BulletRound;
import td.ui.render.ExecutionDraw;
import td.ui.render.LaserDraw;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;

/**
 * The Sniper's shots, painted live in board pixels: its bullets and ricochets by round, the hunting scope's laser and
 * the headsman's cut of an execution. Each is a pure function of its draw's progress or clock and a seed.
 */
final class SniperShotArt {

    /** A bullet's streak is this share of its way, drawn as this many pieces. */
    private static final double STREAK_SHARE = 0.45;
    private static final int STREAK_PIECES = 6;
    /** A round wider than this, in cells, carries a white core down its front half. */
    private static final double CORED_CELLS = 0.04;
    private static final int GLITTER = 5;

    private static final double LASER_CELLS = 0.010;
    private static final double LASER_ALPHA = 0.75;
    private static final double LASER_SHIMMER_HZ = 7;
    private static final Color LASER = new Color(255, 60, 60);
    private static final Color LASER_DOT = new Color(0xffd0c8);

    /** The execution: the cut's share of it, its length and slope, its shards and its red. */
    private static final double EXECUTION_STRIKE = 0.18;
    private static final double EXECUTION_CUT_CELLS = 0.62;
    private static final double EXECUTION_CUT_DEGREES = 62;
    private static final int EXECUTION_SHARDS = 10;
    private static final Color EXECUTION_RED = new Color(0xe0202a);
    private static final Color EXECUTION_STAIN = new Color(0x2a0508);
    private static final Color EXECUTION_PALE = new Color(0xffb0a0);
    private static final Color EXECUTION_HOT = new Color(0xffe8c0);
    /** How flat the ring and stain lie on the ground: as flat as a lobbed shell's shadow. */
    private static final double EXECUTION_GROUND_SQUASH = 0.55;

    private SniperShotArt() {
    }

    /** How a round is drawn: its tail and head colours, its streak's width and its glow's, in cells, and the glow. */
    private enum Look {
        PLAIN(new Color(0x7ccf6a), Color.WHITE, 0.028, 0, 0, Color.WHITE),
        CRIT(Color.WHITE, Color.WHITE, 0.032, 0.07, 0.30, Color.WHITE),
        FOURTH_SHOT(new Color(0xff4a4a), Color.WHITE, 0.052, 0.20, 0.55, new Color(0xff5a5a)),
        HOLLOW_POINT(new Color(0xc0581e), new Color(0xffc27a), 0.052, 0.15, 0.45, new Color(0xff8a3a)),
        SILVER_ROUND(new Color(0x8fb8ff), new Color(0xf4f8ff), 0.052, 0.16, 0.50, new Color(0xb8d4ff));

        private final Color tail;
        private final Color head;
        private final double width;
        private final double glow;
        private final double glowAlpha;
        private final Color halo;

        Look(Color tail, Color head, double width, double glow, double glowAlpha, Color halo) {
            this.tail = tail;
            this.head = head;
            this.width = width;
            this.glow = glow;
            this.glowAlpha = glowAlpha;
            this.halo = halo;
        }
    }

    private static Look lookOf(BulletRound round) {
        return switch (round) {
            case PLAIN -> Look.PLAIN;
            case CRIT -> Look.CRIT;
            case FOURTH_SHOT -> Look.FOURTH_SHOT;
            case HOLLOW_POINT -> Look.HOLLOW_POINT;
            case SILVER_ROUND -> Look.SILVER_ROUND;
        };
    }

    private static Color mixed(Color a, Color b, double share) {
        return new Color((int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * share),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * share),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * share));
    }

    private static BasicStroke round(double width) {
        return new BasicStroke((float) width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    }

    /**
     * The streak from its tail to its head along the bullet's path: its glow, then pieces each wider, brighter and
     * nearer the head colour than the one behind, then a white core on a bold round, then its head's glow. A Silver
     * Round leaves glitter behind.
     */
    static void bullet(Graphics2D g2, BulletDraw bullet) {
        double head = bullet.flown();
        double tail = Math.max(0, head - STREAK_SHARE);
        if (head <= tail) {
            return;
        }
        Look look = lookOf(bullet.round());
        double scale = bullet.cellPixels();
        double alpha = bullet.alpha();
        double[][] p = new double[STREAK_PIECES + 1][];
        for (int i = 0; i <= STREAK_PIECES; i++) {
            p[i] = at(bullet, tail + (head - tail) * i / STREAK_PIECES);
        }
        if (look.glow > 0) {
            for (int i = 0; i < STREAK_PIECES; i++) {
                double share = (i + 1.0) / STREAK_PIECES;
                g2.setStroke(round(look.glow * scale * (0.5 + 0.5 * share)));
                g2.setColor(TowerLifeArt.faded(look.halo, look.glowAlpha * alpha * share * 0.5));
                g2.draw(new Line2D.Double(p[i][0], p[i][1], p[i + 1][0], p[i + 1][1]));
            }
        }
        for (int i = 0; i < STREAK_PIECES; i++) {
            double share = i / (STREAK_PIECES - 1.0);
            g2.setStroke(round(look.width * scale * (0.55 + 0.45 * share)));
            g2.setColor(TowerLifeArt.faded(mixed(look.tail, look.head, share * share), alpha * (0.3 + 0.7 * share)));
            g2.draw(new Line2D.Double(p[i][0], p[i][1], p[i + 1][0], p[i + 1][1]));
        }
        if (look.width > CORED_CELLS) {
            g2.setStroke(round(look.width * scale * 0.38));
            g2.setColor(TowerLifeArt.faded(look.head, alpha * 0.9));
            g2.draw(new Line2D.Double(p[STREAK_PIECES / 2][0], p[STREAK_PIECES / 2][1], p[STREAK_PIECES][0],
                    p[STREAK_PIECES][1]));
        }
        double[] tip = p[STREAK_PIECES];
        if (look.glow > 0) {
            TowerLifeArt.glow(g2, tip[0], tip[1], look.glow * scale * 0.9, look.halo, look.glowAlpha * alpha);
            TowerLifeArt.glow(g2, tip[0], tip[1], look.width * scale * 1.4, Color.WHITE, 0.9 * alpha);
        }
        if (look == Look.SILVER_ROUND) {
            double seconds = bullet.seconds();
            for (int i = 0; i < GLITTER; i++) {
                double t = tail + (head - tail) * ((i + 0.5 + 0.3 * Math.sin(seconds * 9 + i * 2.3)) / (GLITTER + 0.5));
                double[] spot = at(bullet, Math.clamp(t, 0, 1));
                double off = (TowerLifeArt.hash(i, (long) (seconds * 12)) - 0.5) * look.glow * scale;
                double size = scale * 0.012 * (0.6 + 0.8 * TowerLifeArt.hash(i + 9, (long) (seconds * 12)));
                g2.setColor(TowerLifeArt.faded(Color.WHITE, 0.85 * alpha));
                g2.fill(new Ellipse2D.Double(spot[0] + off - size, spot[1] - off - size, size * 2, size * 2));
            }
        }
    }

    /** The point {@code t} of the way along the bullet's path, bowed as a ricochet's is. */
    private static double[] at(BulletDraw bullet, double t) {
        double fromX = bullet.fromX();
        double fromY = bullet.fromY();
        double toX = bullet.toX();
        double toY = bullet.toY();
        double bowX = (fromX + toX) / 2 - (toY - fromY) * bullet.bow();
        double bowY = (fromY + toY) / 2 + (toX - fromX) * bullet.bow();
        double rest = 1 - t;
        return new double[] {rest * rest * fromX + 2 * rest * t * bowX + t * t * toX,
            rest * rest * fromY + 2 * rest * t * bowY + t * t * toY};
    }

    /** A very thin red line from the lens to the enemy, a faint glow along it that shimmers, and a dot where it lands. */
    static void laser(Graphics2D g2, LaserDraw laser) {
        double scale = laser.cellPixels();
        double shimmer = 0.85 + 0.15 * Math.sin(2 * Math.PI * LASER_SHIMMER_HZ * laser.seconds());
        Line2D.Double line = new Line2D.Double(laser.fromX(), laser.fromY(), laser.toX(), laser.toY());
        g2.setStroke(round(LASER_CELLS * scale * 4));
        g2.setColor(TowerLifeArt.faded(LASER, 0.14 * shimmer));
        g2.draw(line);
        g2.setStroke(round(Math.max(0.6, LASER_CELLS * scale)));
        g2.setColor(TowerLifeArt.faded(LASER, LASER_ALPHA * shimmer));
        g2.draw(line);
        TowerLifeArt.glow(g2, laser.toX(), laser.toY(), scale * 0.09, LASER, 0.7 * shimmer);
        double dot = scale * 0.018;
        g2.setColor(TowerLifeArt.faded(LASER_DOT, 0.95));
        g2.fill(new Ellipse2D.Double(laser.toX() - dot, laser.toY() - dot, dot * 2, dot * 2));
    }

    /**
     * The headsman's cut. A blade of white light slashes down across the body in the first instant, red at its edges.
     * Under it, on the ground, a dark stain spreads and a crimson ring snaps out round the enemy's feet. Red-hot shards
     * burst off both sides of the cut, fall, and go out where they land.
     */
    static void execution(Graphics2D g2, ExecutionDraw execution) {
        double progress = execution.progress();
        if (progress < 0 || progress >= 1) {
            return;
        }
        double scale = execution.cellPixels();
        double x = execution.x();
        double groundY = execution.groundY();
        double bodyLift = execution.bodyLift();
        int seed = execution.seed();
        double fade = progress < EXECUTION_STRIKE ? 1 : 1 - (progress - EXECUTION_STRIKE) / (1 - EXECUTION_STRIKE);
        double spread = Math.sqrt(progress);
        double stain = scale * (0.10 + 0.20 * spread);
        g2.setColor(TowerLifeArt.faded(EXECUTION_STAIN, 0.55 * fade));
        g2.fill(new Ellipse2D.Double(x - stain, groundY - stain * EXECUTION_GROUND_SQUASH, stain * 2,
                stain * 2 * EXECUTION_GROUND_SQUASH));
        double ring = scale * (0.14 + 0.40 * spread);
        Ellipse2D ground = new Ellipse2D.Double(x - ring, groundY - ring * EXECUTION_GROUND_SQUASH, ring * 2,
                ring * 2 * EXECUTION_GROUND_SQUASH);
        g2.setStroke(new BasicStroke((float) (scale * (0.05 * (1 - progress) + 0.008))));
        g2.setColor(TowerLifeArt.faded(EXECUTION_RED, 0.35 * fade));
        g2.draw(ground);
        g2.setStroke(new BasicStroke((float) (scale * (0.022 * (1 - progress) + 0.006))));
        g2.setColor(TowerLifeArt.faded(mixed(EXECUTION_RED, EXECUTION_PALE, 0.3 * (1 - progress)), 0.9 * fade));
        g2.draw(ground);
        double y = groundY - bodyLift;
        double cut = Math.toRadians(EXECUTION_CUT_DEGREES);
        double cx = Math.cos(cut);
        double cy = Math.sin(cut);
        double half = scale * EXECUTION_CUT_CELLS / 2;
        double drawn = Math.min(1, progress / EXECUTION_STRIKE);
        double ax = x - cx * half;
        double ay = y - cy * half;
        Line2D.Double blade = new Line2D.Double(ax, ay, ax + cx * half * 2 * drawn, ay + cy * half * 2 * drawn);
        TowerLifeArt.glow(g2, x, y, scale * 0.26 * (0.6 + 0.4 * drawn), EXECUTION_RED, 0.5 * fade);
        g2.setStroke(round(scale * 0.075));
        g2.setColor(TowerLifeArt.faded(EXECUTION_RED, 0.75 * fade));
        g2.draw(blade);
        g2.setStroke(round(scale * 0.030));
        g2.setColor(TowerLifeArt.faded(Color.WHITE, fade));
        g2.draw(blade);
        if (progress < EXECUTION_STRIKE) {
            return;
        }
        double flight = (progress - EXECUTION_STRIKE) / (1 - EXECUTION_STRIKE);
        double out = 1 - (1 - flight) * (1 - flight);
        for (int i = 0; i < EXECUTION_SHARDS; i++) {
            double side = i % 2 == 0 ? 1 : -1;
            double along = (TowerLifeArt.hash(seed, i) - 0.5) * 1.6;
            double turn = (TowerLifeArt.hash(seed + 5L, i) - 0.5) * 0.9;
            double nx = -cy * side + cx * turn;
            double ny = cx * side + cy * turn;
            double norm = Math.hypot(nx, ny);
            nx /= norm;
            ny /= norm;
            double reach = scale * (0.12 + 0.42 * out) * (0.7 + 0.5 * TowerLifeArt.hash(seed + 9L, i));
            double start = bodyLift - cy * half * along;
            double toss = scale * 0.10 * TowerLifeArt.hash(seed + 13L, i);
            double height = Math.max(0, start * (1 - flight) + toss * 4 * flight * (1 - flight));
            double px = x + cx * half * along + nx * reach;
            double py = groundY + ny * reach * EXECUTION_GROUND_SQUASH - height;
            double landed = height <= 0 ? 1 : 0;
            double size = scale * 0.05 * (1 - 0.5 * flight);
            g2.setColor(TowerLifeArt.faded(mixed(EXECUTION_HOT, EXECUTION_RED, Math.min(1, flight * 1.8)),
                    (1 - flight) * (1 - 0.3 * landed)));
            Path2D.Double shard = new Path2D.Double();
            shard.moveTo(px + nx * size * 1.8, py + ny * size * 1.8);
            shard.lineTo(px - ny * size * 0.6, py + nx * size * 0.6);
            shard.lineTo(px - nx * size, py - ny * size);
            shard.lineTo(px + ny * size * 0.6, py - nx * size * 0.6);
            shard.closePath();
            g2.fill(shard);
        }
    }
}
