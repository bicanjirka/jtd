package td.ui;

import td.ui.render.BlastDraw;
import td.ui.render.BoardFlashDraw;
import td.ui.render.EnemyBodyDraw;
import td.ui.render.EnemyDraw;
import td.ui.render.EnemyEmissionDraw;
import td.ui.render.EnemyFadeDraw;
import td.ui.render.ShellTrail;
import td.ui.render.TrailLook;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A Mortar's shells going off, their tails in flight and a nuke's flash over the board. What a blast throws up stands on
 * the board like everything else: each puff of fire or smoke is lit from the top left, its side turned down the screen
 * in shade, and the nearer puffs, lower on the screen, cover the farther ones. What spreads (a shock ring, a scorch)
 * lies flat on the ground. Every particle is a function of the blast's progress and seed; nothing is stored.
 */
final class BlastArt {

    /** A puff's shade, body and highlight discs, offset so the light falls from the top left. */
    private static final double PUFF_SHADE_DROP = 0.16;
    private static final double PUFF_LIT_SHIFT = 0.08;

    static final Color WHITE_HOT = new Color(0xfffaea);
    static final Color YELLOW_FIRE = new Color(0xffd24a);
    static final Color ORANGE_FIRE = new Color(0xff8a2a);
    static final Color RED_FIRE = new Color(0xe2402a);
    static final Tones TAR = new Tones(new Color(0x2a2134), new Color(0x54436a), new Color(0x9a86c2));
    static final Tones ICE = Tones.of(new Color(170, 225, 255));
    private static final Color HEAVY_SMOKE = new Color(0x6e6660);
    private static final Color SOOT = new Color(0x3a3030);
    private static final Color DIRT = new Color(0x6a5038);
    private static final Color SCORCH = new Color(0x120c08);
    private static final Color SHOCK = new Color(0xfffbe8);
    private static final Color TAR_GLINT = new Color(0xf0eaff);
    private static final Color TAR_SMOKE = new Color(0x4a4256);
    private static final Color TAR_FLASH = new Color(0xb49ad8);
    private static final Color VAPOUR = new Color(0xd8eefa);
    private static final Color FROST_FLASH = new Color(0xdff6ff);
    private static final Color NUKE_GLOW = new Color(0xc8f04a);
    private static final Color NUKE_SMOKE = new Color(0xc4c290);
    private static final Color NUKE_DUST = new Color(0xa8a27a);
    private static final Color NUKE_HEAT = new Color(0xffc860);
    private static final Color FLASH_LIGHT = new Color(0xfffdf0);
    private static final Color SILHOUETTE = new Color(0x1a140c);

    private BlastArt() {
    }

    static void paint(Graphics2D g2, BlastDraw blast) {
        switch (blast.look()) {
            case HEAVY -> heavy(g2, blast);
            case NAPALM -> napalm(g2, blast);
            case TAR -> tar(g2, blast);
            case CRYO -> cryo(g2, blast);
            case NUKE -> nuke(g2, blast);
        }
    }

    // ---- shared, also with the payload grounds ---------------------------------------------------------------------

    static Color mix(Color a, Color b, double t) {
        double s = Easing.clamp01(t);
        return new Color((int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * s),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * s),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * s));
    }

    /** A ramp through {@code stops} at {@code t} 0, 1/(n-1), .. 1, held past its ends. */
    private static Color ramp(double t, Color... stops) {
        double at = Easing.clamp01(t) * (stops.length - 1);
        int i = Math.min(stops.length - 2, (int) Math.floor(at));
        return mix(stops[i], stops[i + 1], at - i);
    }

    static double hash(int seed, int i) {
        return TowerLifeArt.hash(seed, i);
    }

    static Color faded(Color color, double alpha) {
        return TowerLifeArt.faded(color, alpha);
    }

    static Ellipse2D disc(double x, double y, double r) {
        return new Ellipse2D.Double(x - r, y - r, r * 2, r * 2);
    }

    /** A flat disc on the ground. */
    static void flat(Graphics2D g2, double x, double y, double r, Color color, double alpha) {
        if (r > 0 && alpha > 0) {
            g2.setColor(faded(color, alpha));
            g2.fill(disc(x, y, r));
        }
    }

    /** A ring on the ground. */
    private static void ring(Graphics2D g2, double x, double y, double r, double width, Color color, double alpha) {
        if (r <= 0 || alpha <= 0) {
            return;
        }
        Stroke stroke = g2.getStroke();
        g2.setStroke(new BasicStroke((float) Math.max(1, width)));
        g2.setColor(faded(color, alpha));
        g2.draw(disc(x, y, r));
        g2.setStroke(stroke);
    }

    /** One ball of fire or smoke: its centre on screen, and how near it is (the nearer, the later it is painted). */
    record Puff(double x, double y, double r, Color color, double alpha, double depth) {

        /** A puff as near as its centre is low on the screen. */
        Puff(double x, double y, double r, Color color, double alpha) {
            this(x, y, r, color, alpha, y);
        }
    }

    /** A ball of fire or smoke standing in the air: its shade side down the screen, its lit rim up and to the left. */
    static void puff(Graphics2D g2, Puff p) {
        if (p.r() <= 0.3 || p.alpha() <= 0) {
            return;
        }
        Tones tones = Tones.of(p.color());
        double r = stepped(p.r());
        double a = Math.round(p.alpha() * ALPHA_STEPS) / (double) ALPHA_STEPS;
        Stamps.paint(g2, new PuffLook(r, p.color().getRGB(), a), (float) p.x(), (float) p.y(), r * 1.3, g -> {
            g.setColor(faded(tones.shade(), a));
            g.fill(disc(r * PUFF_LIT_SHIFT, r * PUFF_SHADE_DROP, r));
            g.setColor(faded(tones.highlight(), a));
            g.fill(disc(-r * PUFF_LIT_SHIFT * 0.5, -r * PUFF_LIT_SHIFT, r * 0.94));
            g.setColor(faded(p.color(), a));
            g.fill(disc(r * PUFF_LIT_SHIFT * 0.5, r * PUFF_LIT_SHIFT * 0.4, r * 0.86));
        });
    }

    /** Puffs back to front. */
    static void puffs(Graphics2D g2, List<Puff> puffs) {
        puffs.sort(Comparator.comparingDouble(Puff::depth));
        for (Puff p : puffs) {
            puff(g2, p);
        }
    }

    /** A flame standing on {@code baseY}: round at its foot, drawn up to a point {@code height} above it. */
    private static Shape tongue(double x, double baseY, double width, double height) {
        Path2D.Double t = new Path2D.Double();
        t.moveTo(x, baseY - height);
        t.curveTo(x + width * 0.25, baseY - height * 0.6, x + width, baseY - height * 0.35, x + width * 0.75,
                baseY - width * 0.1);
        t.quadTo(x, baseY + width * 0.55, x - width * 0.75, baseY - width * 0.1);
        t.curveTo(x - width, baseY - height * 0.35, x - width * 0.25, baseY - height * 0.6, x, baseY - height);
        t.closePath();
        return t;
    }

    /** A three-layer flame standing on the ground: red outside, orange, a yellow heart. */
    static void flame(Graphics2D g2, double x, double baseY, double width, double height, double alpha) {
        if (height <= 0.5 || alpha <= 0) {
            return;
        }
        double w = stepped(width);
        double h = stepped(height);
        double a = Math.round(alpha * ALPHA_STEPS) / (double) ALPHA_STEPS;
        Stamps.paint(g2, new FlameLook(w, h, a), (float) x, (float) baseY, Math.max(w, h) * 1.2, g -> {
            g.setColor(faded(RED_FIRE, a * 0.9));
            g.fill(tongue(0, 0, w, h));
            g.setColor(faded(ORANGE_FIRE, a));
            g.fill(tongue(0, 0, w * 0.68, h * 0.74));
            g.setColor(faded(YELLOW_FIRE, a));
            g.fill(tongue(0, w * 0.05, w * 0.38, h * 0.45));
        });
    }

    // Flames and puffs flicker every frame; their sizes and strengths come in steps, so their pictures are kept.
    private static final double SIZE_STEPS = 2;
    private static final int ALPHA_STEPS = 16;

    private record FlameLook(double width, double height, double alpha) {
    }

    private record PuffLook(double r, int rgb, double alpha) {
    }

    private static double stepped(double size) {
        return Math.round(size * SIZE_STEPS) / SIZE_STEPS;
    }

    /** A shard of ice standing on {@code x, y}: its left face lit, its right in shade. */
    static void shard(Graphics2D g2, double x, double y, double width, double height, double lean, double alpha) {
        if (height <= 0.5 || alpha <= 0) {
            return;
        }
        double tipX = x + lean * height;
        double tipY = y - height;
        Path2D.Double left = new Path2D.Double();
        left.moveTo(tipX, tipY);
        left.lineTo(x, y + width * 0.35);
        left.lineTo(x - width, y - width * 0.2);
        left.closePath();
        Path2D.Double right = new Path2D.Double();
        right.moveTo(tipX, tipY);
        right.lineTo(x + width, y - width * 0.15);
        right.lineTo(x, y + width * 0.35);
        right.closePath();
        Path2D.Double spine = new Path2D.Double();
        spine.moveTo(tipX, tipY);
        spine.lineTo(x + width * 0.35, y);
        spine.lineTo(x, y + width * 0.35);
        spine.lineTo(x - width * 0.3, y);
        spine.closePath();
        g2.setColor(faded(ICE.highlight(), alpha));
        g2.fill(left);
        g2.setColor(faded(ICE.shade(), alpha));
        g2.fill(right);
        g2.setColor(faded(ICE.body(), alpha));
        g2.fill(spine);
    }

    /** A four-point glint. */
    static void glint(Graphics2D g2, double x, double y, double r, Color color, double alpha) {
        if (r <= 0.2 || alpha <= 0) {
            return;
        }
        Path2D.Double star = new Path2D.Double();
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            double reach = i % 2 == 0 ? r : r * 0.25;
            double px = x + Math.cos(a) * reach;
            double py = y + Math.sin(a) * reach;
            if (i == 0) {
                star.moveTo(px, py);
            } else {
                star.lineTo(px, py);
            }
        }
        star.closePath();
        g2.setColor(faded(color, alpha));
        g2.fill(star);
    }

    /** A gob of tar: dark, with a glossy glint up and to the left. */
    static void glob(Graphics2D g2, double x, double y, double r, double alpha) {
        g2.setColor(faded(TAR.shade(), alpha));
        g2.fill(disc(x + r * 0.1, y + r * 0.12, r));
        g2.setColor(faded(TAR.body(), alpha));
        g2.fill(disc(x - r * 0.05, y - r * 0.05, r * 0.8));
        g2.setColor(faded(TAR_GLINT, alpha * 0.9));
        g2.fill(disc(x - r * 0.35, y - r * 0.38, r * 0.24));
    }

    /** Where something thrown out at {@code share} of its flight is: out along the ground, up and back down. */
    private static double[] thrown(double x, double y, double angle, double reach, double height, double share) {
        double s = Easing.clamp01(share);
        return new double[] {x + Math.cos(angle) * reach * s, y + Math.sin(angle) * reach * s,
                height * 4 * s * (1 - s)};
    }

    /** The first bright moment of a blast: a glow round its core and a hot heart. */
    private static void flash(Graphics2D g2, double x, double y, double r, Color color, double share) {
        if (share >= 1) {
            return;
        }
        double fade = 1 - share;
        TowerLifeArt.glow(g2, x, y, r * (0.9 + 0.5 * share), color, 0.85 * fade);
        flat(g2, x, y, r * 0.42 * fade, WHITE_HOT, fade);
    }

    /** A cloud's puff number {@code i} of {@code n}: spread over a disc evenly, the first ones in the middle. */
    private static double spiral(int i, int n) {
        return Math.sqrt((i + 0.5) / n);
    }

    // ---- a Heavy ---------------------------------------------------------------------------------------------------

    private static final int HEAVY_PUFFS = 10;
    private static final int HEAVY_CLODS = 10;

    /** Brief and hard: a white flash, a shock ring, a fireball that cools to smoke as it lifts, clods of earth. */
    private static void heavy(Graphics2D g2, BlastDraw b) {
        double p = b.progress();
        double u = b.unit();
        TowerLifeArt.glow(g2, b.x(), b.y(), b.core() * 1.1, SCORCH, 0.7 * (1 - Easing.inOut(0.5, 1, p)));
        ring(g2, b.x(), b.y(), b.edge() * Easing.outStrong(p / 0.4), u * 0.08 * (1 - p), SHOCK, 0.85 * (1 - Easing.clamp01(p / 0.4)));
        flash(g2, b.x(), b.y(), b.core() * 1.1, YELLOW_FIRE, p / 0.3);
        double grow = Easing.outStrong(p / 0.4);
        double fade = 1 - Easing.inOut(0.6, 1, p);
        List<Puff> puffs = new ArrayList<>();
        for (int i = 0; i < HEAVY_PUFFS; i++) {
            double h = hash(b.seed(), i);
            double size = 0.6 + 0.7 * hash(b.seed(), 100 + i);
            double angle = i * 2.4 + b.seed();
            double inner = 1 - (double) i / HEAVY_PUFFS;
            double out = b.core() * 0.32 * spiral(i, HEAVY_PUFFS) * (0.5 + grow);
            double gx = b.x() + Math.cos(angle) * out;
            double gy = b.y() + Math.sin(angle) * out * 0.75;
            double rise = u * (0.12 + (0.2 + 0.45 * inner) * Easing.outStrong(p * 1.2));
            double r = b.core() * 0.25 * size * (0.45 + 0.75 * grow) * (1 - 0.3 * Easing.inOut(0.6, 1, p));
            Color color = ramp(1.05 - p * (1.3 + 0.6 * h) - 0.15 * (1 - inner), HEAVY_SMOKE, RED_FIRE, ORANGE_FIRE,
                    YELLOW_FIRE, WHITE_HOT);
            puffs.add(new Puff(gx, gy - rise, r, color, fade, gy - rise * 0.3));
        }
        clods(g2, b, HEAVY_CLODS, p * 1.3);
        puffs(g2, puffs);
    }

    /** Clods of earth thrown out over the blast, up and back down onto the ground. */
    private static void clods(Graphics2D g2, BlastDraw b, int count, double share) {
        if (share >= 1) {
            return;
        }
        for (int i = 0; i < count; i++) {
            double h = hash(b.seed(), 40 + i);
            double angle = hash(b.seed(), 60 + i) * 2 * Math.PI;
            double[] at = thrown(b.x(), b.y(), angle, b.edge() * (0.5 + 0.45 * h), b.unit() * (0.6 + 0.7 * h), share);
            double r = b.unit() * (0.03 + 0.03 * hash(b.seed(), 80 + i));
            flat(g2, at[0], at[1] - at[2], r, DIRT, 1);
            flat(g2, at[0] - r * 0.3, at[1] - at[2] - r * 0.3, r * 0.45, mix(DIRT, Color.WHITE, 0.3), 1);
        }
    }

    // ---- napalm ----------------------------------------------------------------------------------------------------

    private static final int NAPALM_FLAMES = 14;
    private static final int NAPALM_PUFFS = 8;
    private static final int NAPALM_DROPS = 9;

    /**
     * Burning jelly thrown wide: a wall of flame that runs out across the ground, a fireball that rolls up into black
     * smoke, and burning gobs that land and burn on.
     */
    private static void napalm(Graphics2D g2, BlastDraw b) {
        double p = b.progress();
        double u = b.unit();
        double spread = Easing.outStrong(p / 0.3);
        TowerLifeArt.glow(g2, b.x(), b.y(), b.edge() * (0.5 + 0.5 * spread), ORANGE_FIRE,
                0.5 * (1 - Easing.inOut(0.5, 1, p)));
        flash(g2, b.x(), b.y(), b.core() * 1.2, ORANGE_FIRE, p / 0.22);
        double burn = Math.pow(1 - p, 0.6);
        List<double[]> flames = new ArrayList<>();
        for (int i = 0; i < NAPALM_FLAMES; i++) {
            double h = hash(b.seed(), i);
            double angle = i * 2.4 + h * 0.4;
            double reach = spiral(i, NAPALM_FLAMES);
            double out = b.edge() * 0.8 * reach * spread;
            flames.add(new double[] {b.x() + Math.cos(angle) * out, b.y() + Math.sin(angle) * out, h, reach, i});
        }
        flames.sort(Comparator.comparingDouble(f -> f[1]));
        List<Puff> back = new ArrayList<>();
        List<Puff> front = new ArrayList<>();
        double grow = Easing.outStrong(p / 0.45);
        for (int i = 0; i < NAPALM_PUFFS; i++) {
            double h = hash(b.seed(), 20 + i);
            double size = 0.7 + 0.6 * hash(b.seed(), 120 + i);
            double angle = i * 2.4 + b.seed() * 0.7;
            double inner = 1 - (double) i / NAPALM_PUFFS;
            double out = b.core() * 0.35 * spiral(i, NAPALM_PUFFS) * (0.5 + grow);
            double gx = b.x() + Math.cos(angle) * out;
            double gy = b.y() + Math.sin(angle) * out * 0.7;
            double rise = u * (0.15 + (0.6 + 0.9 * inner) * Easing.outStrong(p * 1.05));
            double r = b.core() * 0.27 * size * (0.45 + 0.8 * grow);
            Color color = ramp(1 - p * (1.5 + 0.6 * h) - 0.25 * inner, SOOT, RED_FIRE, ORANGE_FIRE, YELLOW_FIRE);
            Puff puff = new Puff(gx, gy - rise, r, color, 1 - Easing.inOut(0.62, 1, p));
            (rise > u * 0.45 ? back : front).add(puff);
        }
        puffs(g2, back);
        for (double[] f : flames) {
            double flicker = 0.8 + 0.2 * Math.sin(p * 37 + f[4] * 2.1 + b.seed());
            double height = u * (0.35 + 0.35 * f[2]) * (1.35 - 0.6 * f[3]) * burn * flicker;
            flame(g2, f[0], f[1], u * (0.13 + 0.05 * f[2]), height, 1 - Easing.inOut(0.8, 1, p));
        }
        puffs(g2, front);
        for (int i = 0; i < NAPALM_DROPS; i++) {
            double h = hash(b.seed(), 40 + i);
            double angle = hash(b.seed(), 60 + i) * 2 * Math.PI;
            double share = p * 2.2 / (0.8 + 0.4 * h);
            double[] at = thrown(b.x(), b.y(), angle, b.edge() * (0.6 + 0.4 * h), u * (0.5 + 0.4 * h), share);
            if (share < 1) {
                flat(g2, at[0], at[1] - at[2], u * 0.055, ORANGE_FIRE, 1);
                flat(g2, at[0] - u * 0.012, at[1] - at[2] - u * 0.012, u * 0.03, YELLOW_FIRE, 1);
            } else {
                double flicker = 0.8 + 0.2 * Math.sin(p * 41 + i * 1.3);
                flame(g2, at[0], at[1], u * 0.06, u * 0.2 * (1 - p) * flicker, 1 - Easing.inOut(0.8, 1, p));
            }
        }
    }

    // ---- tar -------------------------------------------------------------------------------------------------------

    private static final int TAR_SPIKES = 10;
    private static final int TAR_GLOBS = 12;

    /**
     * Thick tar hitting the ground: a crown of it splashes up round the hole and falls back, gobs fly out and splat
     * flat, and a thin dirty smoke lifts off.
     */
    private static void tar(Graphics2D g2, BlastDraw b) {
        double p = b.progress();
        double u = b.unit();
        flash(g2, b.x(), b.y(), b.core() * 0.9, TAR_FLASH, p / 0.18);
        double fade = 1 - Easing.inOut(0.7, 1, p);
        for (int i = 0; i < TAR_GLOBS; i++) {
            double h = hash(b.seed(), 40 + i);
            double share = p * 2.4 / (0.8 + 0.4 * h);
            if (share >= 1) {
                double angle = hash(b.seed(), 60 + i) * 2 * Math.PI;
                double[] at = thrown(b.x(), b.y(), angle, b.edge() * (0.5 + 0.45 * h), 0, 1);
                splat(g2, at[0], at[1], u * (0.08 + 0.05 * h), fade);
            }
        }
        double crown = Math.sin(Math.PI * Easing.clamp01(p / 0.6));
        double ringR = b.core() * (0.22 + 0.32 * Easing.outStrong(p / 0.4));
        splat(g2, b.x(), b.y(), ringR * 1.3, fade);
        List<double[]> spikes = new ArrayList<>();
        for (int i = 0; i < TAR_SPIKES; i++) {
            double h = hash(b.seed(), i);
            double angle = i * 2 * Math.PI / TAR_SPIKES + h * 0.3;
            spikes.add(new double[] {b.x() + Math.cos(angle) * ringR, b.y() + Math.sin(angle) * ringR * 0.8, h,
                    Math.cos(angle)});
        }
        spikes.sort(Comparator.comparingDouble(s -> s[1]));
        for (double[] s : spikes) {
            spike(g2, s[0], s[1], u * 0.075, u * (0.4 + 0.4 * s[2]) * crown, s[3] * 0.3, fade);
        }
        for (int i = 0; i < TAR_GLOBS; i++) {
            double h = hash(b.seed(), 40 + i);
            double share = p * 2.4 / (0.8 + 0.4 * h);
            if (share < 1) {
                double angle = hash(b.seed(), 60 + i) * 2 * Math.PI;
                double[] at = thrown(b.x(), b.y(), angle, b.edge() * (0.5 + 0.45 * h), u * (0.5 + 0.5 * h), share);
                glob(g2, at[0], at[1] - at[2], u * (0.055 + 0.035 * h), 1);
            }
        }
        double smoke = Easing.inOut(0.1, 0.4, p) * (1 - Easing.inOut(0.6, 1, p));
        List<Puff> puffs = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            double h = hash(b.seed(), 90 + i);
            double gx = b.x() + (h - 0.5) * b.core() * 0.5 + Math.sin(p * 4 + i) * u * 0.05;
            puffs.add(new Puff(gx, b.y() - u * (0.35 + 0.7 * p + 0.25 * i), b.core() * (0.14 + 0.1 * p + 0.03 * i),
                    TAR_SMOKE, 0.6 * smoke, -i));
        }
        puffs(g2, puffs);
    }

    /** Tar lying where it splashed: a dark pool with a gloss on its lit side. */
    static void splat(Graphics2D g2, double x, double y, double r, double alpha) {
        g2.setColor(faded(TAR.shade(), 0.95 * alpha));
        g2.fill(new Ellipse2D.Double(x - r, y - r * 0.75, r * 2, r * 1.5));
        g2.setColor(faded(TAR.highlight(), 0.5 * alpha));
        g2.fill(new Ellipse2D.Double(x - r * 0.6, y - r * 0.55, r * 0.7, r * 0.3));
    }

    /** A finger of tar splashed up: a column with a drop at its tip, lit on its left. */
    private static void spike(Graphics2D g2, double x, double y, double width, double height, double lean,
                              double alpha) {
        if (height <= 0.5) {
            return;
        }
        double tipX = x + lean * height;
        double tipY = y - height;
        g2.setColor(faded(TAR.shade(), alpha));
        g2.fill(column(x, y, tipX, tipY, width));
        g2.setColor(faded(TAR.body(), alpha));
        g2.fill(column(x - width * 0.3, y, tipX - width * 0.25, tipY, width * 0.55));
        glob(g2, tipX, tipY, width * 1.25, alpha);
    }

    /** A column {@code width} wide at its foot, waisted, narrowing to its tip. */
    private static Shape column(double x, double y, double tipX, double tipY, double width) {
        Path2D.Double c = new Path2D.Double();
        c.moveTo(x - width, y);
        c.quadTo(x - width * 0.25, (y + tipY) / 2, tipX - width * 0.4, tipY);
        c.lineTo(tipX + width * 0.4, tipY);
        c.quadTo(x + width * 0.25, (y + tipY) / 2, x + width, y);
        c.closePath();
        return c;
    }

    // ---- cryo ------------------------------------------------------------------------------------------------------

    private static final int CRYO_SHARDS = 8;
    private static final int CRYO_GLINTS = 12;

    /**
     * A cryogenic burst: a cold white flash, a low bank of vapour that rolls out along the ground, spikes of ice that
     * jut up at once and melt away, and frost glinting in the air.
     */
    private static void cryo(Graphics2D g2, BlastDraw b) {
        double p = b.progress();
        double u = b.unit();
        double spread = Easing.outStrong(p / 0.5);
        double fade = 1 - Easing.inOut(0.5, 1, p);
        TowerLifeArt.glow(g2, b.x(), b.y(), b.edge() * (0.4 + 0.7 * spread), VAPOUR, 0.4 * fade);
        ring(g2, b.x(), b.y(), b.edge() * spread, u * 0.05, Color.WHITE, 0.6 * fade);
        flash(g2, b.x(), b.y(), b.core() * 1.2, FROST_FLASH, p / 0.22);
        Bank vapour = new Bank(b, b.edge() * 0.72 * spread, u * 0.36 * (0.5 + 0.5 * spread)
                * (1 - 0.5 * Easing.inOut(0.55, 1, p)), VAPOUR, 1 - Easing.inOut(0.75, 1, p));
        bank(g2, vapour, true);
        double jut = Easing.outStrong(p / 0.12) * (1 - Easing.inOut(0.7, 1, p));
        List<double[]> shards = new ArrayList<>();
        for (int i = 0; i < CRYO_SHARDS; i++) {
            double h = hash(b.seed(), i);
            double angle = i * 2 * Math.PI / (CRYO_SHARDS - 1) + h;
            double out = i == 0 ? 0 : b.core() * (0.2 + 0.25 * h);
            shards.add(new double[] {b.x() + Math.cos(angle) * out, b.y() + Math.sin(angle) * out * 0.8, h,
                    i == 0 ? 0 : Math.cos(angle)});
        }
        shards.sort(Comparator.comparingDouble(s -> s[1]));
        for (double[] s : shards) {
            boolean middle = s[3] == 0 && s[0] == b.x();
            double height = u * (middle ? 0.85 : 0.4 + 0.3 * s[2]) * jut;
            shard(g2, s[0], s[1], u * (middle ? 0.13 : 0.08), height, s[3] * 0.4, 1);
        }
        bank(g2, vapour, false);
        for (int i = 0; i < CRYO_GLINTS; i++) {
            double h = hash(b.seed(), 50 + i);
            double angle = hash(b.seed(), 70 + i) * 2 * Math.PI;
            double out = b.edge() * 0.85 * h * spread;
            double twinkle = Math.max(0, Math.sin(p * 18 + i * 1.7));
            glint(g2, b.x() + Math.cos(angle) * out, b.y() + Math.sin(angle) * out - u * (0.2 + 0.5 * h) * p,
                    u * 0.08 * twinkle, Color.WHITE, fade);
        }
    }

    // ---- a nuke ----------------------------------------------------------------------------------------------------

    private static final int NUKE_CAP_RING = 14;
    private static final int NUKE_CAP_TOP = 7;
    private static final int NUKE_STEM = 10;
    private static final int NUKE_SKIRT = 7;
    private static final int NUKE_SURGE = 30;

    /**
     * A nuclear blast, slow and huge: a fireball on the ground that lifts off and rolls into a mushroom cloud on a stem
     * of dust, a shock ring and a base surge running out across the ground, a ring of cloud round the stem as the
     * shock passes. It cools from white heat through fire to the dusty yellow-green of its fallout, and thins away.
     */
    private static void nuke(Graphics2D g2, BlastDraw b) {
        double p = b.progress();
        double u = b.unit();
        double x = b.x();
        double y = b.y();
        double vanish = 1 - Easing.inOut(0.72, 1, p);
        TowerLifeArt.glow(g2, x, y, b.edge() * 1.3, NUKE_HEAT, 0.75 * (1 - Easing.inOut(0, 0.35, p)));
        TowerLifeArt.glow(g2, x, y, b.edge(), SCORCH, 0.6 * Easing.inOut(0.1, 0.35, p) * vanish);
        double shock = p / 0.2;
        ring(g2, x, y, b.edge() * 1.6 * Easing.outStrong(shock), u * 0.12 * (1 - Easing.clamp01(shock)), SHOCK,
                0.9 * (1 - Easing.clamp01(shock)));
        double dust = p / 0.55;
        ring(g2, x, y, b.edge() * 2.1 * Easing.outStrong(dust), u * 0.35, NUKE_DUST, 0.22 * (1 - Easing.clamp01(dust)));

        double heat = 1 - Easing.clamp01(p / 0.4);
        Color fire = ramp(heat, NUKE_SMOKE, ORANGE_FIRE, YELLOW_FIRE, WHITE_HOT);
        double capR = Math.min(b.edge() * 0.5, u * 1.25) * (0.6 + 0.4 * Easing.outStrong(p / 0.45)) * (1 + 0.12 * p);
        double lift = u * (0.2 + 3.9 * Easing.outStrong((p - 0.04) / 0.6));
        double capY = y - lift;
        double roll = p * 2.4;
        double cloudIn = Easing.inOut(0.03, 0.1, p);

        double surgeOut = Easing.outStrong(p / 0.5);
        double surgeShow = 0.85 * Easing.inOut(0, 0.05, p) * (1 - Easing.inOut(0.4, 0.7, p));
        Bank surge = new Bank(b, b.edge() * (0.3 + 0.5 * surgeOut), u * 0.6 * (0.5 + 0.5 * surgeOut)
                * (1 - 0.5 * Easing.inOut(0.3, 0.7, p)), mix(NUKE_DUST, fire, heat * 0.5), surgeShow);
        bank(g2, surge, true);
        if (p < 0.14) {
            double ball = b.edge() * 0.55 * Easing.outStrong(p / 0.05);
            TowerLifeArt.glow(g2, x, y - ball * 0.5, ball * 2, YELLOW_FIRE, 0.8 * (1 - p / 0.14));
            puff(g2, new Puff(x, y - ball * 0.55, ball * (1 - 0.4 * cloudIn), ramp(heat, ORANGE_FIRE, YELLOW_FIRE,
                    WHITE_HOT), 1 - cloudIn, 0));
        }
        double stemShow = cloudIn * vanish;
        List<Puff> cloud = new ArrayList<>();
        for (int i = 0; i < NUKE_SKIRT; i++) {
            double h = hash(b.seed(), 220 + i);
            double angle = i * 2 * Math.PI / NUKE_SKIRT + h;
            double gx = x + Math.cos(angle) * u * 0.4;
            double gy = y + Math.sin(angle) * u * 0.25;
            double r = u * (0.3 + 0.1 * h);
            cloud.add(new Puff(gx, gy - r * 0.6, r, mix(NUKE_DUST, fire, heat * 0.4), stemShow, -1e6 + gy));
        }
        double stemTop = capY + capR * 0.15;
        if (stemShow > 0) {
            stem(g2, x, y, stemTop, u, mix(mix(NUKE_DUST, NUKE_SMOKE, 0.5), fire, heat * 0.6), stemShow);
        }
        for (int i = 0; i < NUKE_STEM; i++) {
            double t = (i + 0.5) / NUKE_STEM;
            double h = hash(b.seed(), 30 + i);
            int side = i % 2 == 0 ? -1 : 1;
            double half = u * (0.42 - 0.14 * t);
            double r = u * (0.24 + 0.08 * h) * (1 - 0.25 * t);
            double cy = y + (stemTop - y) * t;
            Color color = mix(mix(NUKE_DUST, NUKE_SMOKE, t), fire, heat * (0.3 + 0.6 * t));
            cloud.add(new Puff(x + side * half * (0.35 + 0.2 * h), cy, r, color, stemShow, -1e5 + i));
        }
        if (p > 0.08 && p < 0.4) {
            double wilson = (p - 0.08) / 0.32;
            double rx = capR * (0.7 + 1.0 * wilson);
            double wy = y - lift * 0.5;
            Stroke stroke = g2.getStroke();
            g2.setStroke(new BasicStroke((float) (u * 0.1)));
            g2.setColor(faded(Color.WHITE, 0.5 * Math.sin(Math.PI * wilson) * vanish));
            g2.draw(new Ellipse2D.Double(x - rx, wy - rx * 0.26, rx * 2, rx * 0.52));
            g2.setStroke(stroke);
        }
        Color cap = mix(NUKE_SMOKE, fire, heat);
        for (int i = 0; i < NUKE_CAP_RING; i++) {
            double h = hash(b.seed(), 50 + i);
            double angle = i * 2 * Math.PI / NUKE_CAP_RING + roll;
            double cx = x + Math.cos(angle) * capR * 0.7;
            double cy = capY + Math.sin(angle) * capR * 0.24 + capR * 0.1;
            double r = capR * (0.34 + 0.08 * h);
            double under = (1 - Math.sin(angle)) / 2;
            cloud.add(new Puff(cx, cy, r, mix(cap, Tones.of(cap).shade(), 0.25 * under), stemShow,
                    Math.sin(angle) * 10));
        }
        for (int i = 0; i < NUKE_CAP_TOP; i++) {
            double h = hash(b.seed(), 70 + i);
            double angle = i * 2 * Math.PI / NUKE_CAP_TOP + roll * 0.5 + 0.4;
            double cx = x + Math.cos(angle) * capR * 0.38;
            double cy = capY - capR * (0.25 + 0.12 * h) + Math.sin(angle) * capR * 0.12;
            double r = capR * (0.36 + 0.08 * h);
            cloud.add(new Puff(cx, cy, r, mix(cap, Color.WHITE, 0.08), stemShow, -5 + Math.sin(angle)));
        }
        puffs(g2, cloud);
        if (heat > 0) {
            TowerLifeArt.glow(g2, x, capY + capR * 0.25, capR * 0.9, YELLOW_FIRE, 0.55 * heat * cloudIn);
            TowerLifeArt.glow(g2, x, capY, capR * 1.7, NUKE_GLOW, 0.35 * heat * cloudIn);
        }
        bank(g2, surge, false);
    }

    /** A low bank of cloud rolling out in a ring along the ground: its centre, radius, thickness, colour and seed. */
    private record Bank(double x, double y, double radius, double width, Color color, double alpha, int seed) {

        /** A bank rolling out from the middle of {@code blast}. */
        Bank(BlastDraw blast, double radius, double width, Color color, double alpha) {
            this(blast.x(), blast.y(), radius, width, color, alpha, blast.seed());
        }
    }

    private static final int BANK_LUMPS = 28;

    /**
     * Half of a bank: the far half (up the screen) before what stands in the middle, the near half after it. The band
     * is a thick ring, lit along its top and shaded below, with lumps along it so it reads as cloud.
     */
    private static void bank(Graphics2D g2, Bank bank, boolean far) {
        if (bank.alpha() <= 0 || bank.width() <= 0.5 || bank.radius() <= 0) {
            return;
        }
        Tones tones = Tones.of(bank.color());
        Shape clip = g2.getClip();
        double r = bank.radius();
        double w = bank.width();
        g2.clip(new Rectangle2D.Double(bank.x() - r - w * 2, far ? bank.y() - r - w * 2 : bank.y(),
                (r + w * 2) * 2, r + w * 2));
        Stroke stroke = g2.getStroke();
        g2.setStroke(new BasicStroke((float) w));
        g2.setColor(faded(tones.shade(), bank.alpha()));
        g2.draw(disc(bank.x(), bank.y() + w * 0.3, r));
        g2.setColor(faded(tones.highlight(), bank.alpha()));
        g2.draw(disc(bank.x(), bank.y() - w * 0.12, r));
        g2.setStroke(new BasicStroke((float) (w * 0.8)));
        g2.setColor(faded(bank.color(), bank.alpha()));
        g2.draw(disc(bank.x(), bank.y() + w * 0.05, r));
        g2.setStroke(stroke);
        List<Puff> lumps = new ArrayList<>();
        for (int i = 0; i < BANK_LUMPS; i++) {
            double angle = i * 2 * Math.PI / BANK_LUMPS + hash(bank.seed(), 900 + i) * 0.2;
            double ly = bank.y() + Math.sin(angle) * r;
            double lr = w * (0.35 + 0.4 * hash(bank.seed(), 950 + i));
            lumps.add(new Puff(bank.x() + Math.cos(angle) * r, ly - w * 0.25, lr, bank.color(), bank.alpha(), ly));
        }
        puffs(g2, lumps);
        g2.setClip(clip);
    }

    /** A nuke's stem: a column of dust narrowing up to the cloud, lit down its left side. */
    private static void stem(Graphics2D g2, double x, double foot, double top, double unit, Color color,
                             double alpha) {
        double bottom = unit * 0.55;
        double upper = unit * 0.32;
        Tones tones = Tones.of(color);
        g2.setColor(faded(tones.shade(), alpha));
        g2.fill(trapezoid(x, foot, top, bottom, upper));
        g2.setColor(faded(color, alpha));
        g2.fill(trapezoid(x - unit * 0.04, foot, top, bottom * 0.8, upper * 0.8));
        g2.setColor(faded(tones.highlight(), alpha));
        g2.fill(trapezoid(x - bottom * 0.45, foot, top, bottom * 0.3, upper * 0.3));
    }

    private static Shape trapezoid(double x, double foot, double top, double bottomHalf, double topHalf) {
        Path2D.Double t = new Path2D.Double();
        t.moveTo(x - bottomHalf, foot);
        t.curveTo(x - bottomHalf * 0.6, (foot + top) / 2, x - topHalf, top + (foot - top) * 0.2, x - topHalf, top);
        t.lineTo(x + topHalf, top);
        t.curveTo(x + topHalf, top + (foot - top) * 0.2, x + bottomHalf * 0.6, (foot + top) / 2, x + bottomHalf, foot);
        t.closePath();
        return t;
    }

    // ---- tails -----------------------------------------------------------------------------------------------------

    private static final Color NAPALM_GLOW = new Color(0xff5a1e);
    private static final Color TAR_TRAIL_GLOW = new Color(0x9a7ad0);
    private static final Color CRYO_GLOW = new Color(0xaae6ff);
    private static final Color FLARE_GLOW = new Color(0xfff2a8);
    private static final Color FLARE_SPARK = new Color(0xffb43a);
    private static final Color TRAIL_SMOKE = new Color(0x8a8480);
    private static final Color FLARE_SMOKE = new Color(0xd8d4c8);

    /** A tail's glow, its streak and the streak's hottest colour, at its head. */
    private record TrailTones(Color glow, Color body, Color hot) {
    }

    private static TrailTones tonesOf(TrailLook look) {
        return switch (look) {
            case NAPALM -> new TrailTones(NAPALM_GLOW, ORANGE_FIRE, YELLOW_FIRE);
            case TAR -> new TrailTones(TAR_TRAIL_GLOW, TAR.body(), TAR.highlight());
            case CRYO -> new TrailTones(CRYO_GLOW, CRYO_GLOW, Color.WHITE);
            case FLARE -> new TrailTones(FLARE_GLOW, FLARE_GLOW, new Color(0xfffbe0));
            case NUKE -> new TrailTones(NUKE_GLOW, new Color(0xe8f4b0), WHITE_HOT);
        };
    }

    /**
     * A shell's tail, head radius {@code radius}: a soft glow round a hot streak that thins and cools toward its end,
     * with what each kind sheds: smoke off a burning or a tarry shell, frost off a freezing one, sparks and smoke off a
     * flare.
     */
    static void trail(Graphics2D g2, ShellTrail trail, double radius) {
        List<ShellTrail.Point> points = trail.points();
        int n = points.size();
        if (n < 2) {
            return;
        }
        double r = radius * trail.width();
        TrailTones tones = tonesOf(trail.look());
        int seed = trail.seed();
        for (int i = n - 1; i >= 1; i--) {
            double s = (double) i / (n - 1);
            ShellTrail.Point point = points.get(i);
            switch (trail.look()) {
                case NAPALM -> smoke(g2, point, r, s, 0.3, TRAIL_SMOKE, 0.5);
                case TAR -> smoke(g2, point, r, s, 0.2, TAR_SMOKE, 0.55);
                case CRYO -> smoke(g2, point, r, s, 0.3, Color.WHITE, 0.25);
                case FLARE -> smoke(g2, point, r, s, 0.4, FLARE_SMOKE, 0.35);
                case NUKE -> {
                }
            }
        }
        Stroke stroke = g2.getStroke();
        for (int i = n - 2; i >= 0; i--) {
            double s = (double) i / (n - 1);
            segment(g2, points.get(i), points.get(i + 1), r * (3.6 - 2.4 * s), tones.glow(), 0.22 * (1 - s));
        }
        for (int i = n - 2; i >= 0; i--) {
            double s = (double) i / (n - 1);
            double flicker = trail.look() == TrailLook.NAPALM ? 0.8 + 0.4 * hash(seed, i) : 1;
            segment(g2, points.get(i), points.get(i + 1), r * (1.6 - 1.3 * s) * flicker,
                    mix(tones.hot(), tones.body(), s * 1.4), 0.95 * Math.pow(1 - s, 0.6));
        }
        g2.setStroke(stroke);
        for (int k = 0; k < 4; k++) {
            int i = 1 + (int) (hash(seed, k) * (n - 2));
            ShellTrail.Point point = points.get(i);
            double s = (double) i / (n - 1);
            double dx = (hash(seed, 10 + k) - 0.5) * r * 2;
            double fall = r * (0.6 + 1.8 * s);
            switch (trail.look()) {
                case CRYO -> glint(g2, point.x() + dx, point.y() + (hash(seed, 20 + k) - 0.5) * r * 2,
                        r * 0.8 * (1 - s), Color.WHITE, 1 - s);
                case FLARE -> flat(g2, point.x() + dx, point.y() + fall, r * 0.24, FLARE_SPARK, 1 - s);
                case TAR -> glob(g2, point.x() + dx * 0.5, point.y() + fall, r * 0.28, 1 - s);
                case NAPALM -> flat(g2, point.x() + dx, point.y() + fall * 0.6, r * 0.2, YELLOW_FIRE, 1 - s);
                case NUKE -> {
                }
            }
        }
    }

    /** One piece of a tail between two of its points, {@code width} across, round at both ends. */
    private static void segment(Graphics2D g2, ShellTrail.Point from, ShellTrail.Point to, double width, Color color,
                                double alpha) {
        if (width <= 0.2 || alpha <= 0) {
            return;
        }
        g2.setStroke(new BasicStroke((float) width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(faded(color, alpha));
        g2.draw(new Line2D.Double(from.x(), from.y(), to.x(), to.y()));
    }

    /** Smoke shed off a tail behind its first {@code from} of it, rising and spreading as it goes. */
    private static void smoke(Graphics2D g2, ShellTrail.Point point, double r, double s, double from, Color color,
                              double alpha) {
        if (s > from) {
            flat(g2, point.x(), point.y() - r * 1.2 * s, r * (0.5 + 1.1 * s), color, alpha * (1 - s));
        }
    }

    // ---- a nuke's flash --------------------------------------------------------------------------------------------

    /** Below this strength the flash no longer hides the enemies' colours. */
    private static final double SILHOUETTE_FROM = 0.4;
    private static final double FLASH_ALPHA = 0.88;

    /**
     * A nuke's flash over the whole board: a white wash, whitest round the blast, and while it is strong every enemy
     * shows only as a dark silhouette, its shadow thrown away from the flash.
     */
    static void whiteOut(Graphics2D g2, BoardFlashDraw flash, List<EnemyDraw> enemies, int width, int height) {
        double s = flash.strength();
        g2.setColor(faded(FLASH_LIGHT, FLASH_ALPHA * s));
        g2.fillRect(0, 0, width + 1, height + 1);
        TowerLifeArt.glow(g2, flash.x(), flash.y(), Math.max(width, height) * 0.6, Color.WHITE, 0.6 * s);
        double show = Easing.clamp01((s - SILHOUETTE_FROM) / 0.25);
        if (show <= 0) {
            return;
        }
        for (EnemyDraw enemy : enemies) {
            switch (enemy) {
                case EnemyBodyDraw body -> silhouette(g2, flash, body, show * (1 - body.cloakProgress()));
                case EnemyFadeDraw _, EnemyEmissionDraw _ -> {
                }
            }
        }
    }

    private static void silhouette(Graphics2D g2, BoardFlashDraw flash, EnemyBodyDraw body, double alpha) {
        if (alpha <= 0) {
            return;
        }
        EnemyLooks.Row row = EnemyLooks.of(body.look());
        float lift = EnemySprites.lift(body.look(), body.x(), body.y(), body.scale());
        float side = row.form().height() * body.scale();
        double away = Math.atan2(body.y() - flash.y(), body.x() - flash.x());
        double reach = body.scale() * 3;
        double endX = body.x() + Math.cos(away) * reach;
        double endY = body.y() + Math.sin(away) * reach;
        Stroke stroke = g2.getStroke();
        g2.setStroke(new BasicStroke(body.scale() * 0.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setPaint(new GradientPaint(body.x(), body.y(), faded(SILHOUETTE, 0.7 * alpha), (float) endX, (float) endY,
                faded(SILHOUETTE, 0)));
        g2.draw(new Line2D.Double(body.x(), body.y(), endX, endY));
        g2.setStroke(stroke);
        AffineTransform place = AffineTransform.getTranslateInstance(body.x(), body.y() - lift);
        place.rotate(body.facingRadians());
        Shape top = place.createTransformedShape(row.body().at(body.scale()));
        g2.setColor(faded(SILHOUETTE, alpha));
        g2.fill(AffineTransform.getTranslateInstance(0, side).createTransformedShape(top));
        g2.fill(AffineTransform.getTranslateInstance(0, side / 2).createTransformedShape(top));
        g2.fill(top);
    }
}
