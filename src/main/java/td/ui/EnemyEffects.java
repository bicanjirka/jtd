package td.ui;

import td.ui.render.EnemyBurstDraw;
import td.ui.render.EnemyEggDraw;
import td.ui.render.EnemyEmissionDraw;
import td.ui.render.EnemyIceDraw;
import td.ui.render.EnemyTintDraw;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * Paints what an effect draws on an enemy. Every particle is a pure function of the clock and the enemy's seed, so
 * nothing is stored. Fixed sizes here are pixels on screen at the default window: each paint scales by
 * {@link #BOARD_PIXELS_PER_SCREEN_PIXEL} first and reads board sizes through it, so bodies and art keep their proportions.
 */
final class EnemyEffects {

    private static final float BOARD_PIXELS_PER_SCREEN_PIXEL = 0.82f;
    private static final double TAU = Math.PI * 2;

    private static final Color SHIELD_RIM = new Color(0xFF, 0xDC, 0x78);
    private static final Color SHIELD_FLASH = new Color(0xFF, 0xF4, 0xC8);
    private static final Color SHIELD_HIGHLIGHT = new Color(0xFF, 0xF8, 0xE0);
    private static final float EGG_BREATH = 0.012f;
    private static final int POP_ARCS = 9;
    private static final float POP_GROWTH = 0.7f;

    private static final FireColors BURN_FIRE = new FireColors(new Color(0xFF, 0x6A, 0x1A), new Color(0xFF, 0xD3, 0x4D),
            new Color(0xFF, 0xB3, 0x47));
    private static final FireColors SOUL_FIRE = new FireColors(new Color(0x3F, 0x7D, 0xFF), new Color(0xD5, 0xE6, 0xFF),
            new Color(0x9F, 0xC2, 0xFF));
    private static final Color POISON_TINT = new Color(0x8F, 0xC2, 0x1F);
    private static final Color BUBBLE_FILL = new Color(190, 235, 60);
    private static final Color BUBBLE_RIM = new Color(0xC4, 0xEE, 0x4A);
    private static final Color BLOOD_DROP = new Color(0xE3, 0x2D, 0x3C);
    private static final Color BLOOD_SPLAT = new Color(0xB8, 0x1F, 0x2D);
    private static final Color CHILL_TINT = new Color(0x8F, 0xD4, 0xFF);
    private static final Color CHILL_RIM = new Color(0xC8, 0xEC, 0xFF);
    private static final Color FLAKE = new Color(0xD6, 0xF1, 0xFF);
    private static final Color FREEZE_TINT = new Color(0xA8, 0xDC, 0xFF);
    private static final Color ICE_RIM = new Color(240, 251, 255);
    private static final Color HEAL_PLUS = new Color(0x7D, 0xFF, 0xA0);
    private static final Color DAZE_STAR = new Color(0xFF, 0xE4, 0x6A);

    /** How strong a poison tint is: enough to read as sick, little enough to keep the body's own colour. */
    private static final float POISON_TINT_ALPHA = 0.22f;
    /** A flame grows with a body up to this radius; beyond it a bigger body burns wider, not taller, and keeps its badge clear. */
    private static final float TALLEST_FLAME_RADIUS = 12f;
    private static final float FREEZE_TINT_ALPHA = 0.55f;
    /** Below this radius on screen a body emits the least, at {@link #TINY_BODY_EMISSION} of the full amount. */
    private static final float TINY_BODY_RADIUS = 5.5f;
    private static final float FULL_EMISSION_RADIUS = 7.9f;
    private static final float TINY_BODY_EMISSION = 0.4f;
    /** What an almost empty pool still shows, of what a full one does. */
    private static final float EMPTY_POOL_EMISSION = 0.55f;
    private static final int MOST_PLUS_SIGNS = 5;
    private static final int DAZE_STARS = 3;
    private static final int ICE_SHARDS = 8;

    /** The blocks of ice, as vertices at radius one; each enemy keeps one. */
    private static final float[][][] ICE = {
        {{-1.5f, -1.15f}, {-.3f, -1.7f}, {1.25f, -1.4f}, {1.6f, -.1f}, {1.3f, 1.25f}, {-.15f, 1.55f}, {-1.45f, 1.2f}, {-1.7f, 0f}},
        {{-1.55f, 1.2f}, {-1.75f, -.2f}, {-1.15f, -.75f}, {-.95f, -1.9f}, {-.3f, -1.05f}, {.15f, -2.25f}, {.65f, -1.05f}, {1.2f, -1.75f},
            {1.35f, -.45f}, {1.7f, .25f}, {1.45f, 1.2f}, {0f, 1.5f}},
        {{-1.25f, 1.3f}, {-1.6f, .15f}, {-.95f, -1.1f}, {.1f, -2.05f}, {1.05f, -1.25f}, {1.55f, .3f}, {1.05f, 1.35f}, {-.05f, 1.6f}}
    };
    /** How far ice stands off what it encases, on screen. */
    private static final float ICE_MARGIN = 1.5f;
    private static final float[] ICE_FRACTIONS = {0f, 0.55f, 1f};
    private static final Color ICE_TOP = new Color(225, 246, 255);
    private static final Color ICE_MIDDLE = new Color(120, 195, 245);
    private static final Color ICE_BOTTOM = new Color(70, 140, 215);

    private EnemyEffects() {
    }

    /** One fire: its outer flame, its inner flame and its embers. */
    private record FireColors(Color outer, Color inner, Color ember) {
    }

    /** How many blocks of ice there are to choose from. */
    static int iceVariants() {
        return ICE.length;
    }

    /** How far ice of {@code variant} stands above the centre of what it encases at {@code scale}. */
    static float iceAbove(int variant, float scale) {
        float most = 0f;
        for (float[] point : ICE[variant]) {
            most = Math.max(most, -point[1]);
        }
        return most * scale + ICE_MARGIN * BOARD_PIXELS_PER_SCREEN_PIXEL;
    }

    /** How far ice of {@code variant} reaches below the centre of what it encases at {@code scale}. */
    static float iceBeneath(int variant, float scale) {
        float most = 0f;
        for (float[] point : ICE[variant]) {
            most = Math.max(most, point[1]);
        }
        return most * scale + ICE_MARGIN * BOARD_PIXELS_PER_SCREEN_PIXEL;
    }

    /** How far ice of {@code variant} reaches from the centre of what it encases at {@code scale}, in any direction. */
    static float iceReach(int variant, float scale) {
        float most = 0f;
        for (float[] point : ICE[variant]) {
            most = Math.max(most, (float) Math.hypot(point[0], point[1]));
        }
        return most * scale + ICE_MARGIN * 2 * BOARD_PIXELS_PER_SCREEN_PIXEL;
    }

    /** A stable number from 0 to 1 for a seed, so a particle keeps its place and timing for its enemy's whole life. */
    private static double hash(double n) {
        double x = Math.sin(n * 127.1 + 11.7) * 43758.5453;
        return x - Math.floor(x);
    }

    private static Color faded(Color color, double alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) Math.round(Math.clamp(alpha, 0.0, 1.0) * 255));
    }

    private static float toScreen(float boardPixels) {
        return boardPixels / BOARD_PIXELS_PER_SCREEN_PIXEL;
    }

    private static void enter(Graphics2D g2, float x, float y) {
        g2.translate(x, y);
        g2.scale(BOARD_PIXELS_PER_SCREEN_PIXEL, BOARD_PIXELS_PER_SCREEN_PIXEL);
    }

    private static Path2D eggPath(float rx, float ry) {
        Path2D.Float egg = new Path2D.Float();
        egg.moveTo(0, -ry);
        egg.curveTo(rx * 0.72f, -ry, rx, -ry * 0.18f, rx, ry * 0.2f);
        egg.curveTo(rx, ry * 0.72f, rx * 0.58f, ry * 0.92f, 0, ry * 0.92f);
        egg.curveTo(-rx * 0.58f, ry * 0.92f, -rx, ry * 0.72f, -rx, ry * 0.2f);
        egg.curveTo(-rx, -ry * 0.18f, -rx * 0.72f, -ry, 0, -ry);
        egg.closePath();
        return egg;
    }

    static void paintEgg(Graphics2D g2, EnemyEggDraw egg) {
        AffineTransform save = g2.getTransform();
        Stroke stroke = g2.getStroke();
        enter(g2, egg.x(), egg.y());
        float breath = 1f + (float) Math.sin(egg.seconds() * 2.6 + egg.seed()) * EGG_BREATH;
        float rx = toScreen(egg.radiusX()) * breath;
        float ry = toScreen(egg.radiusY()) * (2f - breath);
        g2.setColor(faded(egg.flash() > 0f ? SHIELD_FLASH : SHIELD_RIM, (0.85 + egg.flash() * 0.15) * egg.alpha()));
        g2.setStroke(new BasicStroke(1f + egg.flash() * 0.6f));
        g2.draw(eggPath(rx, ry));
        g2.setColor(faded(SHIELD_HIGHLIGHT, 0.7 * egg.alpha()));
        g2.setStroke(new BasicStroke(1.2f));
        g2.translate(-rx * 0.36f, -ry * 0.5f);
        g2.rotate(-0.75);
        g2.draw(new Arc2D.Float(-rx * 0.24f, -ry * 0.15f, rx * 0.48f, ry * 0.3f, -171f, -162f, Arc2D.OPEN));
        g2.setStroke(stroke);
        g2.setTransform(save);
    }

    static void paintBurst(Graphics2D g2, EnemyBurstDraw burst) {
        AffineTransform save = g2.getTransform();
        Stroke stroke = g2.getStroke();
        enter(g2, burst.x(), burst.y());
        switch (burst.look()) {
            case SHIELD_POP -> paintShieldPop(g2, burst);
            case ICE_SHARDS -> paintIceShards(g2, burst);
        }
        g2.setStroke(stroke);
        g2.setTransform(save);
    }

    /** The egg flying apart: pieces of its rim rushing outward and a pale flash where it was. */
    private static void paintShieldPop(Graphics2D g2, EnemyBurstDraw burst) {
        float p = burst.progress();
        float rx = toScreen(burst.radiusX());
        float ry = toScreen(burst.radiusY());
        float grow = 1f + p * POP_GROWTH;
        g2.setStroke(new BasicStroke(1.4f));
        g2.setColor(faded(SHIELD_RIM, (1f - p) * burst.alpha()));
        for (int i = 0; i < POP_ARCS; i++) {
            double from = TAU * i / POP_ARCS + 0.3;
            double to = from + TAU / POP_ARCS * 0.42 * (1 - p * 0.5);
            g2.draw(new Line2D.Double(Math.cos(from) * rx * grow, Math.sin(from) * ry * grow,
                    Math.cos(to) * rx * grow, Math.sin(to) * ry * grow));
        }
        float flash = 0.5f - p * 1.6f;
        if (flash > 0f) {
            g2.setColor(faded(SHIELD_FLASH, flash * burst.alpha()));
            g2.fill(eggPath(rx * (1f + p * 0.2f), ry * (1f + p * 0.2f)));
        }
    }

    /** The ice breaking: splinters thrown out from where it stood, falling a little as they go. */
    private static void paintIceShards(Graphics2D g2, EnemyBurstDraw burst) {
        float p = burst.progress();
        float rx = toScreen(burst.radiusX());
        float ry = toScreen(burst.radiusY());
        for (int i = 0; i < ICE_SHARDS; i++) {
            double s = burst.seed() + i * 5.3;
            double angle = TAU * (i + hash(s)) / ICE_SHARDS;
            double reach = 0.55 + (1 - (1 - p) * (1 - p)) * (0.7 + hash(s + 1) * 0.5);
            float size = (float) (1.6 + hash(s + 2) * 2.2) * (1f - p * 0.4f);
            double fall = p * p * 7;
            AffineTransform save = g2.getTransform();
            g2.translate(Math.cos(angle) * rx * reach, Math.sin(angle) * ry * reach + fall);
            g2.rotate(angle + p * 5 * (hash(s + 3) - 0.5));
            Path2D.Float shard = new Path2D.Float();
            shard.moveTo(size, 0);
            shard.lineTo(-size * 0.6f, -size * 0.5f);
            shard.lineTo(-size * 0.6f, size * 0.5f);
            shard.closePath();
            g2.setColor(faded(i % 3 == 0 ? Color.WHITE : FLAKE, (1f - p) * burst.alpha()));
            g2.fill(shard);
            g2.setTransform(save);
        }
    }

    /** What the amount a body emits is cut to by its size, so a swarm of tiny bodies doesn't become noise. */
    private static float sizeShare(float r) {
        return Math.clamp(TINY_BODY_EMISSION + (r - TINY_BODY_RADIUS) / (FULL_EMISSION_RADIUS - TINY_BODY_RADIUS)
                * (1f - TINY_BODY_EMISSION), TINY_BODY_EMISSION, 1f);
    }

    private static Path2D teardrop(double x, double y, double w, double h) {
        Path2D.Double drop = new Path2D.Double();
        drop.moveTo(x, y - h);
        drop.curveTo(x + w, y - h * 0.35, x + w, y, x, y);
        drop.curveTo(x - w, y, x - w, y - h * 0.35, x, y - h);
        drop.closePath();
        return drop;
    }

    static void paintEmission(Graphics2D g2, EnemyEmissionDraw emission) {
        AffineTransform save = g2.getTransform();
        Stroke stroke = g2.getStroke();
        enter(g2, emission.x(), emission.y());
        float r = toScreen(emission.scale());
        switch (emission.look()) {
            case BURN -> paintFire(g2, emission, r, BURN_FIRE);
            case SOULFIRE -> paintFire(g2, emission, r, SOUL_FIRE);
            case POISON -> paintBubbles(g2, emission, r);
            case BLEED -> paintBlood(g2, emission, r);
            case CHILL -> paintFlakes(g2, emission, r);
            case HEAL -> paintPlusSigns(g2, emission, r);
            case DAZE -> paintStars(g2, emission, r);
        }
        g2.setStroke(stroke);
        g2.setTransform(save);
    }

    /** How much a pool emits: its fullness between a visible minimum and everything, within what the body's size allows. */
    private static float poolEmission(float amount, float r) {
        return sizeShare(r) * (EMPTY_POOL_EMISSION + (1f - EMPTY_POOL_EMISSION) * amount);
    }

    /** Flames licking upward and embers drifting off them. */
    private static void paintFire(Graphics2D g2, EnemyEmissionDraw e, float r, FireColors fire) {
        double t = e.seconds();
        float emit = poolEmission(e.amount(), r);
        float tall = Math.min(r, TALLEST_FLAME_RADIUS);
        int flames = Math.max(1, Math.round((4f + r / 7f) * emit));
        for (int i = 0; i < flames; i++) {
            double s = e.seed() + i * 7.3;
            double p = frac(t * (1.5 + hash(s) * 0.8) + hash(s + 1));
            double x = (hash(s + 2) * 2 - 1) * r * 0.85 + Math.sin(t * 7 + s) * r * 0.06;
            double y = -r * (0.05 + hash(s + 3) * 0.55) - p * tall * 1.05;
            double size = (1 - p) * tall * 0.6 + 1;
            g2.setColor(faded(fire.outer(), Math.min(1.0, (1 - p) * 1.5) * 0.95 * e.alpha()));
            g2.fill(teardrop(x, y, size * 0.62, size * 1.7));
            g2.setColor(faded(fire.inner(), Math.min(1.0, (1 - p) * 1.3) * e.alpha()));
            g2.fill(teardrop(x, y - size * 0.05, size * 0.3, size * 0.95));
        }
        int embers = Math.round(2f * emit);
        for (int i = 0; i < embers; i++) {
            double s = e.seed() + i * 3.1 + 40;
            double p = frac(t * 0.9 + hash(s));
            g2.setColor(faded(fire.ember(), (1 - p) * e.alpha()));
            g2.fill(new Rectangle2D.Double((hash(s + 1) * 2 - 1) * r + Math.sin(t * 4 + s) * 2, -r * 0.8 - p * tall * 2.1, 1.4, 1.4));
        }
    }

    /** Slow bubbles rising and popping: slow where fire is fast. */
    private static void paintBubbles(Graphics2D g2, EnemyEmissionDraw e, float r) {
        double t = e.seconds();
        int bubbles = Math.max(1, Math.round(4f * poolEmission(e.amount(), r)));
        g2.setStroke(new BasicStroke(1f));
        for (int i = 0; i < bubbles; i++) {
            double s = e.seed() + i * 5.7;
            double p = frac(t * 0.55 + hash(s));
            double x = (hash(s + 1) * 2 - 1) * r * 0.85 + Math.sin(t * 2.4 + s) * 1.8;
            double y = -r * 0.1 - p * r * 1.75;
            double pop = p > 0.86 ? (p - 0.86) / 0.14 : 0;
            double size = (1.1 + hash(s + 2) * 1.8 + p * 1.1) * (1 + pop * 0.9);
            double alpha = (1 - pop) * Math.min(1.0, p * 6) * e.alpha();
            Shape bubble = new Ellipse2D.Double(x - size, y - size, size * 2, size * 2);
            g2.setColor(faded(BUBBLE_FILL, 0.35 * alpha));
            g2.fill(bubble);
            g2.setColor(faded(BUBBLE_RIM, alpha));
            g2.draw(bubble);
        }
    }

    /** Drops falling and splatting just under the body; falling tells it apart from everything that rises. */
    private static void paintBlood(Graphics2D g2, EnemyEmissionDraw e, float r) {
        double t = e.seconds();
        int drops = Math.max(1, Math.round(3f * sizeShare(r)));
        for (int i = 0; i < drops; i++) {
            double s = e.seed() + i * 4.4;
            double p = frac(t * 0.85 + hash(s));
            double x = (hash(s + 1) * 2 - 1) * r * 0.75;
            if (p < 0.8) {
                double q = p / 0.8;
                g2.setColor(faded(BLOOD_DROP, e.alpha()));
                g2.fill(teardrop(x, r * 0.75 + q * q * r * 1.15 + 1, 1.7, 4.2));
            } else {
                g2.setColor(faded(BLOOD_SPLAT, (1 - p) / 0.2 * 0.8 * e.alpha()));
                g2.fill(new Ellipse2D.Double(x - 2.8, r * 1.9 - 1, 5.6, 2));
            }
        }
    }

    /** Flakes falling around the body, more of them as the chill deepens. */
    private static void paintFlakes(Graphics2D g2, EnemyEmissionDraw e, float r) {
        double t = e.seconds();
        int flakes = Math.max(1, Math.round((2 + Math.round(e.amount() * 4)) * sizeShare(r)));
        g2.setStroke(new BasicStroke(0.9f));
        for (int i = 0; i < flakes; i++) {
            double s = e.seed() + i * 8.2;
            double p = frac(t * 0.42 + hash(s));
            AffineTransform save = g2.getTransform();
            g2.translate((hash(s + 1) * 2 - 1) * r * 1.2 + Math.sin(t * 1.6 + s) * 2, -r * 1.1 + p * r * 2.5);
            g2.rotate(t + s);
            g2.setColor(faded(FLAKE, Math.sin(p * Math.PI) * 0.95 * e.alpha()));
            double arm = 1.5 + hash(s + 2);
            for (int spoke = 0; spoke < 3; spoke++) {
                double q = spoke * Math.PI / 3;
                g2.draw(new Line2D.Double(Math.cos(q) * arm, Math.sin(q) * arm, -Math.cos(q) * arm, -Math.sin(q) * arm));
            }
            g2.setTransform(save);
        }
    }

    /** Small plus signs rising; each cycle one comes up somewhere new, so a slow heal never looks like a fixture. */
    private static void paintPlusSigns(Graphics2D g2, EnemyEmissionDraw e, float r) {
        double t = e.seconds();
        int signs = Math.max(1, Math.round((1 + (MOST_PLUS_SIGNS - 1) * e.amount()) * sizeShare(r)));
        for (int i = 0; i < signs; i++) {
            double s = e.seed() + i * 6.1;
            double cycle = t * 0.7 + (double) i / signs + hash(s) * 0.2;
            double p = frac(cycle);
            double x = (hash(s + Math.floor(cycle)) * 2 - 1) * r * 0.95;
            double y = -r * 0.15 - p * r * 1.9;
            double arm = 1.5 + 1.4 * Math.sin(p * Math.PI);
            g2.setColor(faded(HEAL_PLUS, Math.sin(p * Math.PI) * e.alpha()));
            g2.fill(new Rectangle2D.Double(x - arm, y - arm * 0.34, arm * 2, arm * 0.68));
            g2.fill(new Rectangle2D.Double(x - arm * 0.34, y - arm, arm * 0.68, arm * 2));
        }
    }

    /** Stars circling the head, nearer ones larger and brighter. */
    private static void paintStars(Graphics2D g2, EnemyEmissionDraw e, float r) {
        double t = e.seconds();
        double rx = r * 0.95 + 2.5;
        double ry = 2.8 + r * 0.12;
        double cy = -r * 1.25 - 3;
        for (int i = 0; i < DAZE_STARS; i++) {
            double angle = t * 3.6 + i * TAU / DAZE_STARS;
            double depth = (Math.sin(angle) + 1) / 2;
            AffineTransform save = g2.getTransform();
            g2.translate(Math.cos(angle) * rx, cy + Math.sin(angle) * ry);
            g2.setColor(faded(DAZE_STAR, (0.45 + 0.55 * depth) * e.alpha()));
            float size = (float) (1.7 + 1.2 * depth);
            g2.fill(Java2DFrameRenderer.starShape(5, size, size * 0.45f));
            g2.setTransform(save);
        }
    }

    static void paintTint(Graphics2D g2, EnemyTintDraw tint) {
        AffineTransform save = g2.getTransform();
        Stroke stroke = g2.getStroke();
        g2.translate(tint.x(), tint.y());
        g2.rotate(tint.facingRadians());
        Shape body = EnemyLooks.of(tint.body()).body().at(tint.scale());
        switch (tint.tint()) {
            case POISON -> {
                g2.setColor(faded(POISON_TINT, POISON_TINT_ALPHA * tint.alpha()));
                g2.fill(body);
            }
            case CHILL -> {
                g2.setColor(faded(CHILL_TINT, (0.1 + tint.strength() * 0.34) * tint.alpha()));
                g2.fill(body);
                g2.setColor(faded(CHILL_RIM, (0.35 + tint.strength() * 0.55) * tint.alpha()));
                g2.setStroke(new BasicStroke(BOARD_PIXELS_PER_SCREEN_PIXEL));
                g2.draw(body);
            }
            case FREEZE -> {
                g2.setColor(faded(FREEZE_TINT, FREEZE_TINT_ALPHA * tint.alpha()));
                g2.fill(body);
            }
        }
        g2.setStroke(stroke);
        g2.setTransform(save);
    }

    /** A translucent block lit from above, with facets from one hub and a glint that moves around its corners. */
    static void paintIce(Graphics2D g2, EnemyIceDraw ice) {
        AffineTransform save = g2.getTransform();
        Stroke stroke = g2.getStroke();
        Paint paint = g2.getPaint();
        enter(g2, ice.x(), ice.y());
        float r = toScreen(ice.scale());
        float[][] unit = ICE[ice.variant()];
        float[][] points = new float[unit.length][2];
        Path2D.Float block = new Path2D.Float();
        for (int i = 0; i < unit.length; i++) {
            points[i][0] = unit[i][0] * r + Math.signum(unit[i][0]) * ICE_MARGIN;
            points[i][1] = unit[i][1] * r + Math.signum(unit[i][1]) * ICE_MARGIN;
            if (i == 0) {
                block.moveTo(points[i][0], points[i][1]);
            } else {
                block.lineTo(points[i][0], points[i][1]);
            }
        }
        block.closePath();
        float alpha = ice.alpha();
        g2.setPaint(new LinearGradientPaint(0f, -r * 2f, 0f, r * 1.6f + 0.01f, ICE_FRACTIONS,
                new Color[] {faded(ICE_TOP, 0.72 * alpha), faded(ICE_MIDDLE, 0.46 * alpha), faded(ICE_BOTTOM, 0.62 * alpha)}));
        g2.fill(block);
        g2.setPaint(paint);
        float hubX = -r * 0.2f;
        float hubY = -r * 0.3f;
        g2.setColor(faded(Color.WHITE, 0.5 * alpha));
        g2.setStroke(new BasicStroke(0.9f));
        for (int i = 0; i < points.length; i += 2) {
            g2.draw(new Line2D.Float(hubX, hubY, points[i][0], points[i][1]));
        }
        Path2D.Float facet = new Path2D.Float();
        facet.moveTo(points[0][0], points[0][1]);
        facet.lineTo(points[1][0], points[1][1]);
        facet.lineTo(hubX, hubY);
        facet.closePath();
        g2.setColor(faded(Color.WHITE, 0.4 * alpha));
        g2.fill(facet);
        g2.setColor(faded(ICE_RIM, 0.95 * alpha));
        g2.setStroke(new BasicStroke(1.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(block);
        double glint = ice.seconds() * 0.8 + ice.seed();
        float[] corner = points[(int) (Math.floor(glint) * 3 % points.length)];
        g2.translate(corner[0], corner[1]);
        g2.setColor(faded(Color.WHITE, Math.sin(frac(glint) * Math.PI) * alpha));
        g2.fill(Java2DFrameRenderer.starShape(4, 3.4f, 1.1f));
        g2.setStroke(stroke);
        g2.setTransform(save);
    }

    private static double frac(double value) {
        return value - Math.floor(value);
    }
}
