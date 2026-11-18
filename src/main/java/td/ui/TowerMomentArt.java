package td.ui;

import td.ui.render.CasingDraw;
import td.ui.render.EmbersDraw;
import td.ui.render.LoadedShell;
import td.ui.render.ShadowDraw;
import td.ui.render.ShellCarryDraw;
import td.ui.render.SteamDraw;
import td.ui.render.TowerCue;
import td.ui.render.TowerPose;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * A tower's moments, painted live in board pixels on the tick clock: a rifle's spent cartridge, a nozzle's steam, a
 * shell carried into a mortar, a frenzied turret's embers and the Sundial's shadow. Each is a pure function of its
 * draw's progress, so nothing is kept between frames.
 */
final class TowerMomentArt {

    /** A spent cartridge flies for this long, and steam is let off for this long once a reload starts. */
    static final double CASING_SECONDS = 0.45;
    static final double STEAM_SECONDS = 0.9;
    private static final Color CASING_BRASS = new Color(0xe8c060);
    private static final int STEAM_PUFFS = 5;
    private static final Color STEAM = new Color(0xdfe6ee);

    /** A carried shell rises this far (cells) above the higher of its two ends. */
    private static final double MORTAR_ARC_PEAK = 0.26;
    /** A volley's shells each fly a quarter of the reload, a single load its middle half. */
    private static final double VOLLEY_SHARE = 0.25;

    private static final int FRENZY_EMBERS = 7;
    private static final double FRENZY_EMBER_SECONDS = 0.9;
    private static final double FRENZY_EMBER_RISE = 1.0;
    private static final Color FRENZY_RED = new Color(0xff3a2a);
    private static final Color FRENZY_HOT = new Color(0xffe0a0);

    /**
     * The Sundial: the band of sun-lit ground and the shadow in it, as half-widths in cells at the stone's foot and at
     * the shadow's tip. Each soft edge is drawn as this many nested layers.
     */
    private static final double SUNDIAL_LIGHT_ROOT = 0.26;
    private static final double SUNDIAL_LIGHT_TIP = 0.56;
    private static final double SUNDIAL_UMBRA_ROOT = 0.10;
    private static final double SUNDIAL_UMBRA_TIP = 0.04;
    private static final double SUNDIAL_PENUMBRA_ROOT = 0.13;
    private static final double SUNDIAL_PENUMBRA_TIP = 0.34;
    private static final int SUNDIAL_SOFT_STEPS = 6;
    private static final Color SUNDIAL_DARK = new Color(0x050309);
    private static final float SUNDIAL_DARK_ALPHA = 0.92f;
    private static final float SUNDIAL_LIGHT_ALPHA = 0.62f;
    private static final float SUNDIAL_LIGHT_TIP_ALPHA = 0.12f;
    private static final float SUNDIAL_EDGE_ALPHA = 0.45f;
    private static final double SUNDIAL_EDGE_CELLS = 0.02;

    private TowerMomentArt() {
    }

    /** Out to the rifle's right, up and over, spinning, down to the ground, gone. */
    static void casing(Graphics2D g2, CasingDraw casing) {
        double progress = casing.progress();
        if (progress < 0 || progress >= 1) {
            return;
        }
        double scale = casing.cellPixels();
        double heading = casing.headingRadians();
        double breech = casing.breechCells();
        double side = heading + Math.PI / 2;
        double out = 0.30 * (1 - (1 - progress) * (1 - progress));
        double high = breech + 0.9 * progress - (breech + 0.9) * progress * progress;
        double x = casing.centerX() + (Math.cos(side) * out - Math.cos(heading) * 0.06 * progress) * scale;
        double groundY = casing.centerY() + (Math.sin(side) * out - Math.sin(heading) * 0.06 * progress) * scale;
        double alpha = progress < 0.8 ? 1 : (1 - progress) / 0.2;
        g2.setColor(TowerLifeArt.faded(Color.BLACK, 0.35 * alpha));
        g2.fill(new Ellipse2D.Double(x - scale * 0.025, groundY - scale * 0.012, scale * 0.05, scale * 0.024));
        AffineTransform save = g2.getTransform();
        g2.translate(x, groundY - TowerFoundations.LIFT * high * scale);
        g2.rotate(progress * 2.5 * 2 * Math.PI);
        g2.setColor(TowerLifeArt.faded(CASING_BRASS, alpha));
        g2.fill(new Rectangle2D.Double(-scale * 0.032, -scale * 0.013, scale * 0.064, scale * 0.026));
        g2.setColor(TowerLifeArt.faded(Color.WHITE, 0.7 * alpha));
        g2.fill(new Rectangle2D.Double(-scale * 0.032, -scale * 0.013, scale * 0.064, scale * 0.009));
        g2.setTransform(save);
    }

    /** Puffs that leave one after another, rise, drift, swell and thin out. */
    static void steam(Graphics2D g2, SteamDraw steam) {
        double progress = steam.progress();
        if (progress < 0 || progress >= 1) {
            return;
        }
        double scale = steam.cellPixels();
        for (int i = 0; i < STEAM_PUFFS; i++) {
            double age = (progress - i * 0.11) / 0.55;
            if (age <= 0 || age >= 1) {
                continue;
            }
            double px = steam.x() + scale * (0.035 * Math.sin(i * 2.4 + age * 3)
                    + (TowerLifeArt.hash(steam.seed(), i) - 0.5) * 0.06);
            double py = steam.y() - scale * (0.03 + 0.30 * age);
            double alpha = 0.62 * (1 - age) * Math.min(1, age * 6);
            TowerLifeArt.glow(g2, px, py, scale * (0.05 + 0.085 * age), STEAM, alpha);
            TowerLifeArt.glow(g2, px, py, scale * (0.025 + 0.04 * age), Color.WHITE, alpha * 0.6);
        }
    }

    /**
     * A shell lifted off its kind's place on the rack and carried on a smooth parabola into the muzzle, drawn as the
     * shell in flight is at the rack's size, its small shadow running under it. A volley's shells go in back to front,
     * so the one fired first goes in last.
     */
    static void shellCarry(Graphics2D g2, ShellCarryDraw carry) {
        double reload = carry.reload();
        boolean volley = carry.volley();
        double length = volley ? VOLLEY_SHARE : 2 * VOLLEY_SHARE;
        double from = volley ? Math.floor(reload / VOLLEY_SHARE) * VOLLEY_SHARE : VOLLEY_SHARE;
        int slot = (int) Math.floor(reload / VOLLEY_SHARE);
        if (carry.load().isEmpty() || reload < from || reload >= from + length
                || (volley && slot >= carry.load().size())) {
            return;
        }
        LoadedShell kind = volley ? carry.load().get(carry.load().size() - 1 - slot) : carry.load().getFirst();
        int tier = carry.tier();
        double u = (reload - from) / length;
        double s = u * u * (3 - 2 * u);
        double[] a = TowerFoundations.mortarPiece(tier, TowerFoundations.mortarPlace(carry.stock(), kind),
                carry.stock().size());
        double[] m = TowerFoundations.mortarMouth(tier, TowerPose.idle().with(TowerCue.LONG_BARREL,
                carry.longBarrel()));
        double endZ = m[2] + TowerFoundations.MORTAR_SHELL * 0.4;
        double x = a[0] + (m[0] - a[0]) * s;
        double y = a[1] + (m[1] - a[1]) * s;
        double peak = Math.max(a[2], endZ) + MORTAR_ARC_PEAK;
        double z = (1 - s) * (1 - s) * a[2] + 2 * (1 - s) * s * (2 * peak - (a[2] + endZ) / 2) + s * s * endZ;
        double scale = carry.cellPixels();
        double radius = TowerFoundations.MORTAR_SHELL * scale;
        double floor = TowerFoundations.mortarFloor(tier);
        double cx = carry.centerX();
        double cy = carry.centerY();
        g2.setColor(TowerLifeArt.faded(Color.BLACK, 0.30 * (1 - 0.5 * Math.sin(Math.PI * s))));
        g2.fill(new Ellipse2D.Double(cx + x * scale - radius,
                cy + (y - TowerFoundations.LIFT * floor) * scale - radius * 0.45, radius * 2, radius * 0.9));
        TowerSprites.paintShell(g2, kind, (float) (cx + x * scale),
                (float) (cy + (y - TowerFoundations.LIFT * z) * scale), (float) radius);
    }

    /** Red sparks that leave the turret one after another, rise, waver and go out. */
    static void embers(Graphics2D g2, EmbersDraw embers) {
        double scale = embers.cellPixels();
        int seed = embers.seed();
        for (int i = 0; i < FRENZY_EMBERS; i++) {
            double t = embers.seconds() / FRENZY_EMBER_SECONDS + (double) i / FRENZY_EMBERS
                    + TowerLifeArt.hash(seed, i) * 0.3;
            long life = (long) Math.floor(t);
            double age = t - life;
            double bearing = TowerLifeArt.hash(life * 13 + i, seed) * 2 * Math.PI;
            double out = scale * 0.16 * (0.4 + 0.6 * TowerLifeArt.hash(life * 7 + i, seed + 3L));
            double px = embers.x() + Math.cos(bearing) * out + Math.sin(age * 7 + i) * scale * 0.025;
            double py = embers.y() + Math.sin(bearing) * out * 0.5
                    - TowerFoundations.LIFT * FRENZY_EMBER_RISE * age * scale;
            double alpha = Math.min(1, age * 5) * (1 - age);
            double size = scale * (0.036 - 0.014 * age);
            TowerLifeArt.glow(g2, px, py, size * 3.0, FRENZY_RED, 0.75 * alpha);
            g2.setColor(TowerLifeArt.faded(mixed(FRENZY_HOT, FRENZY_RED, Math.min(1, age * 1.6)), alpha));
            Path2D.Double spark = new Path2D.Double();
            spark.moveTo(px, py - size * 1.6);
            spark.lineTo(px + size * 0.7, py);
            spark.lineTo(px, py + size * 1.1);
            spark.lineTo(px - size * 0.7, py);
            spark.closePath();
            g2.fill(spark);
        }
    }

    private static Color mixed(Color a, Color b, double share) {
        return new Color((int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * share),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * share),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * share));
    }

    /**
     * The Sundial as a real shadow. The floors are near black, so a shadow alone would not show: the low sun lights a
     * band of the ground along the shadow's line, and the stone's shadow lies dark down the middle of it, sharp and as
     * wide as the stone at its foot, narrowing toward the stone's top while its blurred edge widens and pales with
     * distance. The leading side, where the hit lands, carries a faint lit line.
     */
    static void sundial(Graphics2D g2, ShadowDraw shadow, Color veil, Color umbra) {
        AffineTransform save = g2.getTransform();
        Shape clip = g2.getClip();
        g2.translate(shadow.originX(), shadow.originY());
        g2.rotate(shadow.headingRadians());
        double scale = shadow.cellPixels();
        double reach = shadow.reach();
        double alpha = shadow.alpha();
        lightBand(g2, reach, scale, alpha, veil);
        if (shadow.inFieldUmbra()) {
            float r = shadow.fieldRadius();
            g2.clip(new Ellipse2D.Float(-r, -r, r * 2, r * 2));
            lightBand(g2, reach, scale, alpha, umbra);
            g2.setClip(clip);
        }
        float end = (float) Math.max(1, reach);
        for (int i = 0; i < SUNDIAL_SOFT_STEPS; i++) {
            double t = (double) i / (SUNDIAL_SOFT_STEPS - 1);
            Shape shade = wedge(reach, scale * (SUNDIAL_UMBRA_ROOT + (SUNDIAL_PENUMBRA_ROOT - SUNDIAL_UMBRA_ROOT) * t),
                    scale * (SUNDIAL_UMBRA_TIP + (SUNDIAL_PENUMBRA_TIP - SUNDIAL_UMBRA_TIP) * t));
            double a = alpha / SUNDIAL_SOFT_STEPS * 2;
            g2.setPaint(new LinearGradientPaint(0f, 0f, end, 0f, new float[] {0f, 0.55f, 1f}, new Color[] {
                TowerLifeArt.faded(SUNDIAL_DARK, SUNDIAL_DARK_ALPHA * a),
                TowerLifeArt.faded(SUNDIAL_DARK, SUNDIAL_DARK_ALPHA * 0.75 * a),
                TowerLifeArt.faded(SUNDIAL_DARK, SUNDIAL_DARK_ALPHA * 0.30 * a)}));
            g2.fill(shade);
        }
        Color edge = new Color((veil.getRed() + 510) / 3, (veil.getGreen() + 510) / 3, (veil.getBlue() + 510) / 3);
        g2.setPaint(new LinearGradientPaint(0f, 0f, end, 0f, new float[] {0f, 1f}, new Color[] {
            TowerLifeArt.faded(edge, SUNDIAL_EDGE_ALPHA * alpha), TowerLifeArt.faded(edge, 0)}));
        g2.setStroke(new BasicStroke((float) (SUNDIAL_EDGE_CELLS * scale), BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));
        double rim = (SUNDIAL_UMBRA_ROOT + SUNDIAL_PENUMBRA_ROOT) / 2 * scale;
        double rimTip = (SUNDIAL_UMBRA_TIP + SUNDIAL_PENUMBRA_TIP) / 2 * scale;
        g2.draw(new Line2D.Double(scale * 0.12, -rim, reach, -rimTip));
        g2.setTransform(save);
    }

    /** The band of sun-lit ground along the shadow's line, soft at its edges, fading toward its end. */
    private static void lightBand(Graphics2D g2, double reach, double scale, double alpha, Color light) {
        float end = (float) Math.max(1, reach);
        for (int i = 0; i < SUNDIAL_SOFT_STEPS; i++) {
            double t = (double) i / (SUNDIAL_SOFT_STEPS - 1);
            Shape band = wedge(reach, scale * SUNDIAL_LIGHT_ROOT * (1 - 0.45 * t),
                    scale * SUNDIAL_LIGHT_TIP * (1 - 0.45 * t));
            double a = alpha / SUNDIAL_SOFT_STEPS * 2;
            g2.setPaint(new LinearGradientPaint(0f, 0f, end, 0f, new float[] {0f, 1f}, new Color[] {
                TowerLifeArt.faded(light, SUNDIAL_LIGHT_ALPHA * a),
                TowerLifeArt.faded(light, SUNDIAL_LIGHT_TIP_ALPHA * a)}));
            g2.fill(band);
        }
    }

    /** A wedge along +x from 0 to {@code reach}, {@code root} either side at its foot and {@code tip} at its end. */
    private static Shape wedge(double reach, double root, double tip) {
        Path2D.Double p = new Path2D.Double();
        p.moveTo(0, -root);
        p.lineTo(reach, -tip);
        p.quadTo(reach + tip * 0.6, 0, reach, tip);
        p.lineTo(0, root);
        p.quadTo(-root * 0.6, 0, 0, -root);
        p.closePath();
        return p;
    }
}
