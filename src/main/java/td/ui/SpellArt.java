package td.ui;

import td.ui.render.CurseDraw;
import td.ui.render.EnemyBodyDraw;
import td.ui.render.HexGlyph;
import td.ui.render.HexRuneDraw;
import td.ui.render.Palette;
import td.ui.render.ShockDraw;
import td.ui.render.SpellBlastDraw;
import td.ui.render.SpellOrbDraw;
import td.ui.render.ThunderboltDraw;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.EnumMap;
import java.util.Map;

/**
 * What the spellcasting Bursts paint: the Stormcaller's ball lightning, its shocks and its Thunderstrike, the Hexer's
 * orb, its curses and runes, and both forks' blasts.
 */
final class SpellArt {

    static final Color STORM_GLOW = new Color(0x5a96ff);
    static final Color STORM_BODY = new Color(0xaad7ff);
    static final Color ARCANE_GLOW = new Color(0xa85aff);
    static final Color ARCANE_BODY = new Color(0xcd96ff);
    static final Color ARCANE_HOT = new Color(0xffecff);
    private static final Color STORM_DEEP = new Color(0x1c3c9a);
    private static final Color SHOCK_BLACK = new Color(0x0c0c14);
    private static final Color SHOCK_RIM = new Color(0x78c8ff);
    private static final Color BONE = new Color(0xeef6ff);
    private static final Color RUNE_BACK = new Color(0x140a1e);

    /** How often a crackle takes new joints, a second. */
    private static final double CRACKLE_HZ = 6;
    /** A shock flickers between black and lit this many times over its length. */
    private static final int SHOCK_FLICKERS = 4;
    /** The share of a moment its first flash lasts. */
    private static final double FLASH_SHARE = 0.25;
    /** A spell's blast: its inner ring runs out over this share of it, its outer one starts here. */
    private static final double INNER_RING_SHARE = 0.45;
    private static final double OUTER_RING_START = 0.3;
    /** The Stormcaller's arcs leap out over this share of its blast. */
    private static final double ARC_SHARE = 0.35;
    private static final int STORM_ARCS = 4;
    /** The Hexer's star: each line is drawn over this share of its blast and fades over the next, a frame or two. */
    private static final int PENTAGRAM_LINES = 5;
    private static final double PENTAGRAM_LINE_SHARE = 0.13;
    private static final double PENTAGRAM_LINE_LINGER = 0.08;

    /** Each glyph at a size of one, built once. */
    private static final Map<HexGlyph, Shape> GLYPHS = glyphs();

    private SpellArt() {
    }

    // ---- glyphs ----------------------------------------------------------------------------------------------------

    private static Map<HexGlyph, Shape> glyphs() {
        Map<HexGlyph, Shape> glyphs = new EnumMap<>(HexGlyph.class);
        for (HexGlyph glyph : HexGlyph.values()) {
            glyphs.put(glyph, switch (glyph) {
                case DOOM -> skull();
                case BLIGHT -> drop();
                case BACKLASH -> brokenHeart();
            });
        }
        return glyphs;
    }

    private static Shape skull() {
        Area skull = new Area(new Ellipse2D.Double(-0.78, -0.95, 1.56, 1.4));
        skull.add(new Area(new Rectangle2D.Double(-0.46, 0.2, 0.92, 0.62)));
        skull.subtract(new Area(new Ellipse2D.Double(-0.56, -0.38, 0.42, 0.44)));
        skull.subtract(new Area(new Ellipse2D.Double(0.14, -0.38, 0.42, 0.44)));
        Path2D.Double nose = new Path2D.Double();
        nose.moveTo(0, 0.08);
        nose.lineTo(0.13, 0.32);
        nose.lineTo(-0.13, 0.32);
        nose.closePath();
        skull.subtract(new Area(nose));
        for (int i = -1; i <= 1; i += 2) {
            skull.subtract(new Area(new Rectangle2D.Double(i * 0.16 - 0.04, 0.56, 0.08, 0.3)));
        }
        return skull;
    }

    private static Shape drop() {
        Path2D.Double drop = new Path2D.Double();
        drop.moveTo(0, -1);
        drop.curveTo(0.2, -0.55, 0.68, -0.15, 0.68, 0.3);
        drop.curveTo(0.68, 0.72, 0.36, 0.98, 0, 0.98);
        drop.curveTo(-0.36, 0.98, -0.68, 0.72, -0.68, 0.3);
        drop.curveTo(-0.68, -0.15, -0.2, -0.55, 0, -1);
        drop.closePath();
        Area area = new Area(drop);
        area.subtract(new Area(new Ellipse2D.Double(-0.42, 0.12, 0.28, 0.36)));
        area.add(new Area(new Ellipse2D.Double(0.62, -0.62, 0.3, 0.3)));
        area.add(new Area(new Ellipse2D.Double(0.82, -0.98, 0.18, 0.18)));
        return area;
    }

    private static Shape brokenHeart() {
        Path2D.Double heart = new Path2D.Double();
        heart.moveTo(0, -0.42);
        heart.curveTo(0.3, -1.02, 1.08, -0.86, 0.98, -0.22);
        heart.curveTo(0.9, 0.28, 0.32, 0.62, 0, 0.98);
        heart.curveTo(-0.32, 0.62, -0.9, 0.28, -0.98, -0.22);
        heart.curveTo(-1.08, -0.86, -0.3, -1.02, 0, -0.42);
        heart.closePath();
        Path2D.Double crack = new Path2D.Double();
        crack.moveTo(0, -0.5);
        crack.lineTo(-0.2, -0.12);
        crack.lineTo(0.16, 0.18);
        crack.lineTo(-0.1, 0.5);
        crack.lineTo(0.04, 1.1);
        Area area = new Area(heart);
        area.subtract(new Area(new BasicStroke(0.16f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)
                .createStrokedShape(crack)));
        return area;
    }

    /** {@code glyph} centred on {@code x, y}, {@code size} pixels from its centre to its edge. */
    static Shape glyph(HexGlyph glyph, double x, double y, double size) {
        AffineTransform place = AffineTransform.getTranslateInstance(x, y);
        place.scale(size, size);
        return place.createTransformedShape(GLYPHS.get(glyph));
    }

    /** The colour of the hex {@code glyph} stands for, its status mark's. */
    static Color hexColor(HexGlyph glyph) {
        return Java2DFrameRenderer.colorFor(switch (glyph) {
            case DOOM -> Palette.STATUS_MARKER_DOOM;
            case BLIGHT -> Palette.STATUS_MARKER_BLIGHT;
            case BACKLASH -> Palette.STATUS_MARKER_BACKLASH;
        });
    }

    // ---- runes and curses ------------------------------------------------------------------------------------------

    /**
     * A hex's rune over its enemy: its glyph in the hex's colour on a dark seal, breathing, inside a ring of rune
     * strokes that turns.
     */
    static void rune(Graphics2D g2, HexRuneDraw rune) {
        Color color = hexColor(rune.glyph());
        double s = rune.scale();
        double breath = 0.5 + 0.5 * Math.sin(rune.seconds() * 3.2 + rune.glyph().ordinal() * 2.1);
        TowerLifeArt.glow(g2, rune.x(), rune.y(), s * 2.6, color, 0.35 + 0.2 * breath);
        g2.setColor(TowerLifeArt.faded(RUNE_BACK, 0.6));
        g2.fill(BlastArt.disc(rune.x(), rune.y(), s * 1.45));
        runeRing(g2, rune.x(), rune.y(), s * 1.55, s * 0.16, rune.seconds() * 0.9, color, 0.9);
        g2.setColor(BlastArt.mix(color, Color.WHITE, 0.25 * breath));
        g2.fill(glyph(rune.glyph(), rune.x(), rune.y(), s));
    }

    /** A ring of six rune strokes, turned {@code turn} radians, with the gaps between them. */
    private static void runeRing(Graphics2D g2, double x, double y, double r, double width, double turn, Color color,
                                 double alpha) {
        Stroke stroke = g2.getStroke();
        g2.setStroke(new BasicStroke((float) Math.max(0.8, width), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(TowerLifeArt.faded(color, alpha));
        double start = Math.toDegrees(turn);
        for (int i = 0; i < 6; i++) {
            g2.draw(new Arc2D.Double(x - r, y - r, r * 2, r * 2, start + i * 60, 38, Arc2D.OPEN));
            double a = Math.toRadians(start + i * 60 + 49);
            g2.draw(new Line2D.Double(x + Math.cos(a) * r * 0.86, y - Math.sin(a) * r * 0.86,
                    x + Math.cos(a) * r * 1.14, y - Math.sin(a) * r * 1.14));
        }
        g2.setStroke(stroke);
    }

    /** A curse in flight: its tail, then its head, a dark seal ringed in its hex's colour with the rune at its heart. */
    static void curse(Graphics2D g2, CurseDraw curse) {
        Color color = hexColor(curse.glyph());
        double r = curse.radius();
        BlastArt.trail(g2, curse.trail(), r);
        TowerLifeArt.glow(g2, curse.x(), curse.y(), r * 3, color, 0.7);
        g2.setColor(TowerLifeArt.faded(BlastArt.mix(RUNE_BACK, color, 0.25), 0.92));
        g2.fill(BlastArt.disc(curse.x(), curse.y(), r));
        runeRing(g2, curse.x(), curse.y(), r, r * 0.14, curse.seconds() * 2.4, color, 1);
        g2.setColor(BlastArt.mix(color, Color.WHITE, 0.35));
        g2.fill(glyph(curse.glyph(), curse.x(), curse.y(), r * 0.66));
    }

    // ---- orbs ------------------------------------------------------------------------------------------------------

    /** A spell in flight: its shadow, its tail, then its head. */
    static void orb(Graphics2D g2, SpellOrbDraw orb) {
        double r = orb.radius();
        double x = orb.x();
        double y = orb.y() - orb.lift();
        BlastArt.flat(g2, orb.x(), orb.y(), r * 0.9, SHOCK_BLACK, 0.28);
        BlastArt.trail(g2, orb.trail(), r);
        switch (orb.look()) {
            case BALL_LIGHTNING -> ballLightning(g2, x, y, r, orb.seconds());
            case ARCANE_ORB -> arcaneOrb(g2, x, y, r, orb.seconds());
        }
    }

    private static void ballLightning(Graphics2D g2, double x, double y, double r, double seconds) {
        TowerLifeArt.glow(g2, x, y, r * 3, STORM_GLOW, 0.5);
        g2.setColor(TowerLifeArt.faded(STORM_BODY, 0.9));
        g2.fill(BlastArt.disc(x, y, r));
        TowerLifeArt.glow(g2, x, y, r * 1.1, Color.WHITE, 0.9);
        int frame = (int) Math.floor(seconds * CRACKLE_HZ);
        Stroke stroke = g2.getStroke();
        g2.setStroke(new BasicStroke((float) Math.max(0.8, r * 0.18), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int k = 0; k < 2; k++) {
            double a = BlastArt.hash(frame, k) * Math.PI * 2;
            g2.setColor(TowerLifeArt.faded(k % 2 == 0 ? Color.WHITE : STORM_BODY, 0.55));
            g2.draw(zigzag(x + Math.cos(a) * r * 0.4, y + Math.sin(a) * r * 0.4, x + Math.cos(a) * r * 1.6,
                    y + Math.sin(a) * r * 1.6, 2, r * 0.3, frame * 31 + k));
        }
        g2.setStroke(stroke);
    }

    private static void arcaneOrb(Graphics2D g2, double x, double y, double r, double seconds) {
        TowerLifeArt.glow(g2, x, y, r * 2.8, ARCANE_GLOW, 0.5);
        g2.setColor(TowerLifeArt.faded(BlastArt.mix(ARCANE_GLOW, RUNE_BACK, 0.35), 0.95));
        g2.fill(BlastArt.disc(x, y, r));
        Stroke stroke = g2.getStroke();
        g2.setStroke(new BasicStroke((float) Math.max(0.8, r * 0.2), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double turn = Math.toDegrees(seconds * 2.5);
        g2.setColor(ARCANE_BODY);
        g2.draw(new Arc2D.Double(x - r * 0.62, y - r * 0.62, r * 1.24, r * 1.24, turn, 130, Arc2D.OPEN));
        g2.setColor(ARCANE_HOT);
        g2.draw(new Arc2D.Double(x - r * 0.62, y - r * 0.62, r * 1.24, r * 1.24, turn + 180, 130, Arc2D.OPEN));
        g2.setStroke(stroke);
        TowerLifeArt.glow(g2, x, y, r * 0.7, ARCANE_HOT, 0.9);
        for (int k = 0; k < 3; k++) {
            double a = seconds * 1.6 + k * Math.PI * 2 / 3;
            BlastArt.glint(g2, x + Math.cos(a) * r * 1.5, y + Math.sin(a) * r * 0.85, r * 0.35, ARCANE_HOT, 0.5);
        }
    }

    // ---- the shock -------------------------------------------------------------------------------------------------

    /**
     * An enemy shocked as in a cartoon: it flickers black inside an electric rim, sparks jumping off it, and with
     * {@code skeleton} its bones show through.
     */
    static void shock(Graphics2D g2, ShockDraw shock) {
        EnemyBodyDraw body = shock.body();
        double p = shock.progress();
        double alpha = (1 - body.cloakProgress()) * (p < 0.7 ? 1 : 1 - (p - 0.7) / 0.3);
        if (alpha <= 0) {
            return;
        }
        boolean dark = ((int) Math.floor(p * SHOCK_FLICKERS * 2)) % 2 == 0;
        EnemyLooks.Row row = EnemyLooks.of(body.look());
        float lift = EnemySprites.lift(body.look(), body.x(), body.y(), body.scale());
        float side = row.form().height() * body.scale();
        AffineTransform place = AffineTransform.getTranslateInstance(body.x(), body.y() - lift);
        place.rotate(body.facingRadians());
        Shape top = place.createTransformedShape(row.body().at(body.scale()));
        Area silhouette = new Area(top);
        silhouette.add(new Area(AffineTransform.getTranslateInstance(0, side).createTransformedShape(top)));
        silhouette.add(new Area(AffineTransform.getTranslateInstance(0, side / 2).createTransformedShape(top)));
        Rectangle2D bounds = silhouette.getBounds2D();
        double reach = Math.max(bounds.getWidth(), bounds.getHeight()) / 2;
        TowerLifeArt.glow(g2, bounds.getCenterX(), bounds.getCenterY(), reach * 2.2, STORM_GLOW, 0.8 * alpha);
        g2.setColor(TowerLifeArt.faded(SHOCK_BLACK, (dark ? 0.95 : 0.55) * alpha));
        g2.fill(silhouette);
        if (shock.skeleton()) {
            Shape clip = g2.getClip();
            g2.clip(silhouette);
            skeleton(g2, bounds, (dark ? 1 : 0.6) * alpha);
            g2.setClip(clip);
        }
        Stroke stroke = g2.getStroke();
        double rim = Math.max(1, body.scale() * 0.14);
        g2.setStroke(new BasicStroke((float) (rim * 2.2), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(TowerLifeArt.faded(SHOCK_RIM, 0.55 * alpha));
        g2.draw(silhouette);
        g2.setStroke(new BasicStroke((float) rim, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(TowerLifeArt.faded(dark ? Color.WHITE : SHOCK_RIM, alpha));
        g2.draw(silhouette);
        int frame = (int) Math.floor(p * SHOCK_FLICKERS * 4);
        g2.setStroke(new BasicStroke((float) Math.max(0.8, rim * 0.8), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int k = 0; k < 4; k++) {
            double a = BlastArt.hash(shock.seed() + frame, k) * Math.PI * 2;
            double cx = bounds.getCenterX();
            double cy = bounds.getCenterY();
            g2.setColor(TowerLifeArt.faded(k % 2 == 0 ? Color.WHITE : SHOCK_RIM, alpha));
            g2.draw(zigzag(cx + Math.cos(a) * reach * 0.8, cy + Math.sin(a) * reach * 0.8,
                    cx + Math.cos(a) * reach * 1.6, cy + Math.sin(a) * reach * 1.6, 3, reach * 0.25,
                    shock.seed() * 7 + frame * 13 + k));
        }
        g2.setStroke(stroke);
    }

    /** A cartoon skeleton standing in {@code bounds}: skull, spine, ribs, pelvis, arms and legs. */
    private static void skeleton(Graphics2D g2, Rectangle2D bounds, double alpha) {
        double h = Math.min(bounds.getHeight(), bounds.getWidth() * 1.4) * 0.92;
        double cx = bounds.getCenterX();
        double top = bounds.getCenterY() - h / 2;
        double u = h / 10;
        Stroke stroke = g2.getStroke();
        g2.setColor(TowerLifeArt.faded(BONE, alpha));
        g2.setStroke(new BasicStroke((float) Math.max(0.8, u * 0.55), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D.Double bones = new Path2D.Double();
        bones.moveTo(cx, top + u * 2.4);
        bones.lineTo(cx, top + u * 6.4);
        for (int i = 0; i < 3; i++) {
            double y = top + u * (3.3 + i * 0.9);
            double w = u * (1.7 - i * 0.25);
            bones.moveTo(cx - w, y + u * 0.3);
            bones.quadTo(cx, y - u * 0.35, cx + w, y + u * 0.3);
        }
        bones.moveTo(cx - u * 1.9, top + u * 2.9);
        bones.lineTo(cx - u * 2.6, top + u * 5.2);
        bones.lineTo(cx - u * 2.2, top + u * 6.6);
        bones.moveTo(cx + u * 1.9, top + u * 2.9);
        bones.lineTo(cx + u * 2.6, top + u * 5.2);
        bones.lineTo(cx + u * 2.2, top + u * 6.6);
        bones.moveTo(cx - u * 1.9, top + u * 2.9);
        bones.lineTo(cx + u * 1.9, top + u * 2.9);
        bones.moveTo(cx - u * 0.8, top + u * 6.8);
        bones.lineTo(cx - u * 1.1, top + u * 8.4);
        bones.lineTo(cx - u * 1.0, top + u * 9.8);
        bones.moveTo(cx + u * 0.8, top + u * 6.8);
        bones.lineTo(cx + u * 1.1, top + u * 8.4);
        bones.lineTo(cx + u * 1.0, top + u * 9.8);
        g2.draw(bones);
        g2.fill(new Ellipse2D.Double(cx - u * 1.2, top + u * 6.2, u * 2.4, u * 1.1));
        Area skull = new Area(new Ellipse2D.Double(cx - u * 1.05, top, u * 2.1, u * 1.9));
        skull.add(new Area(new Rectangle2D.Double(cx - u * 0.62, top + u * 1.3, u * 1.24, u * 0.8)));
        skull.subtract(new Area(new Ellipse2D.Double(cx - u * 0.7, top + u * 0.7, u * 0.55, u * 0.55)));
        skull.subtract(new Area(new Ellipse2D.Double(cx + u * 0.15, top + u * 0.7, u * 0.55, u * 0.55)));
        g2.fill(skull);
        g2.setStroke(stroke);
    }

    // ---- the Thunderstrike -----------------------------------------------------------------------------------------

    /**
     * A Thunderstrike: a great bolt standing from the board's top edge onto its enemy, a glow round a lit core with its
     * shaded side down the screen and forks off it, striking twice as it fades, over a flash and a ring on the ground.
     */
    static void thunderbolt(Graphics2D g2, ThunderboltDraw bolt) {
        double p = bolt.progress();
        double cell = bolt.scale();
        double restrike = p > 0.32 && p < 0.42 ? 1 : 0;
        double strength = Math.max(restrike, p < FLASH_SHARE ? 1 : 1 - (p - FLASH_SHARE) / (1 - FLASH_SHARE));
        double x = bolt.x();
        double y = bolt.y();
        double ground = 1 - p;
        TowerLifeArt.glow(g2, x, y, cell * (1.4 + p * 1.6), STORM_GLOW, 0.8 * ground);
        BlastArt.flat(g2, x, y, cell * 0.55, Color.WHITE, 0.6 * strength * ground);
        Stroke stroke = g2.getStroke();
        double ring = cell * (0.4 + 1.8 * Easing.out(p));
        g2.setStroke(new BasicStroke((float) Math.max(1, cell * 0.08)));
        g2.setColor(TowerLifeArt.faded(STORM_BODY, 0.8 * ground));
        g2.draw(new Ellipse2D.Double(x - ring, y - ring * 0.5, ring * 2, ring));
        if (strength > 0.02) {
            Path2D.Double main = boltPath(x, y, cell, bolt.seed() + (p > 0.32 ? 1 : 0));
            stroke(g2, main, cell * 1.1, STORM_GLOW, 0.22 * strength);
            stroke(g2, main, cell * 0.5, STORM_GLOW, 0.4 * strength);
            AffineTransform shade = AffineTransform.getTranslateInstance(cell * 0.06, cell * 0.05);
            stroke(g2, shade.createTransformedShape(main), cell * 0.2, STORM_DEEP, 0.85 * strength);
            stroke(g2, main, cell * 0.2, STORM_BODY, strength);
            stroke(g2, main, cell * 0.08, Color.WHITE, strength);
            for (int k = 0; k < 3; k++) {
                double from = y * (0.3 + 0.18 * k + 0.1 * BlastArt.hash(bolt.seed(), 40 + k));
                double side = BlastArt.hash(bolt.seed(), 50 + k) < 0.5 ? -1 : 1;
                Shape fork = zigzag(x + side * cell * 0.2, from, x + side * cell * (1 + k * 0.3),
                        from + cell * (1.2 + k * 0.4), 4, cell * 0.25, bolt.seed() * 3 + k);
                stroke(g2, fork, cell * 0.25, STORM_GLOW, 0.35 * strength);
                stroke(g2, fork, cell * 0.06, STORM_BODY, strength);
            }
            TowerLifeArt.glow(g2, x, y - cell * 0.3, cell * 1.6, Color.WHITE, 0.85 * strength);
        }
        g2.setStroke(stroke);
    }

    /** The bolt from the top edge above {@code x} down to {@code y}, its joints swinging less as it nears the ground. */
    private static Path2D.Double boltPath(double x, double y, double cell, int seed) {
        int joints = Math.max(6, (int) Math.ceil(y / (cell * 0.6)));
        Path2D.Double path = new Path2D.Double();
        double startX = x + (BlastArt.hash(seed, 99) - 0.5) * cell * 1.5;
        path.moveTo(startX, 0);
        for (int i = 1; i < joints; i++) {
            double t = (double) i / joints;
            double swing = cell * 0.45 * (1 - t * 0.7);
            path.lineTo(startX + (x - startX) * t + (BlastArt.hash(seed, i) - 0.5) * 2 * swing, y * t);
        }
        path.lineTo(x, y);
        return path;
    }

    private static void stroke(Graphics2D g2, Shape shape, double width, Color color, double alpha) {
        if (alpha <= 0) {
            return;
        }
        g2.setStroke(new BasicStroke((float) Math.max(0.8, width), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(TowerLifeArt.faded(color, alpha));
        g2.draw(shape);
    }

    // ---- blasts ----------------------------------------------------------------------------------------------------

    /** A spellcasting Burst's blast, marking where its full damage ends and where its edge does. */
    static void blast(Graphics2D g2, SpellBlastDraw blast) {
        switch (blast.look()) {
            case STORM -> stormBlast(g2, blast);
            case HEX -> hexBlast(g2, blast);
        }
    }

    /**
     * The inner ring of a spell's blast, running out to the core early in it and fading as it gets there, then the
     * outer one, running from the core to the edge; a blast with no edge share shows only the inner. Each is handed to
     * {@code ring} with its radius and strength while it shows.
     */
    private static void blastRings(SpellBlastDraw blast, RingPainter ring) {
        double p = blast.progress();
        double inner = Easing.clamp01(p / INNER_RING_SHARE);
        if (inner < 1) {
            ring.paint(blast.core() * Easing.outStrong(inner), 1 - inner);
        }
        double outer = Easing.clamp01((p - OUTER_RING_START) / (1 - OUTER_RING_START));
        if (blast.edge() > blast.core() && outer > 0 && outer < 1) {
            ring.paint(blast.core() + (blast.edge() - blast.core()) * Easing.outStrong(outer), 1 - outer);
        }
    }

    @FunctionalInterface
    private interface RingPainter {
        void paint(double radius, double strength);
    }

    /** A brief flash where a spell lands. */
    private static void blastFlash(Graphics2D g2, SpellBlastDraw blast, Color color) {
        double p = blast.progress();
        if (p < FLASH_SHARE) {
            TowerLifeArt.glow(g2, blast.x(), blast.y(), blast.core() * 0.9, color, 0.7 * (1 - p / FLASH_SHARE));
        }
    }

    /**
     * An electric blast: a flash, a few arcs of lightning leaping out of the landing, and crackling rings running out to
     * the core and then the edge, their crackle changing every few frames.
     */
    private static void stormBlast(Graphics2D g2, SpellBlastDraw blast) {
        double p = blast.progress();
        double x = blast.x();
        double y = blast.y();
        double cell = blast.scale();
        int frame = blast.seed() * 17 + (int) Math.floor(p * 10);
        blastFlash(g2, blast, Color.WHITE);
        Stroke stroke = g2.getStroke();
        blastRings(blast, (r, strength) -> {
            Shape ring = jaggedRing(x, y, r, cell * 0.04, frame);
            stroke(g2, ring, cell * 0.1, STORM_GLOW, 0.35 * strength);
            stroke(g2, ring, cell * 0.03, Color.WHITE, 0.85 * strength);
        });
        double arcs = p / ARC_SHARE;
        if (arcs < 1) {
            double reach = blast.core() * (0.4 + 0.6 * Easing.outStrong(arcs));
            for (int k = 0; k < STORM_ARCS; k++) {
                double a = (k + BlastArt.hash(frame, k)) * 2 * Math.PI / STORM_ARCS;
                Shape arc = zigzag(x, y, x + Math.cos(a) * reach, y + Math.sin(a) * reach, 2, reach * 0.2, frame + k);
                stroke(g2, arc, cell * 0.08, STORM_GLOW, 0.4 * (1 - arcs));
                stroke(g2, arc, cell * 0.03, Color.WHITE, 1 - arcs);
            }
        }
        g2.setStroke(stroke);
    }

    /**
     * A briefly summoned pentagram: a flash, its star drawn line by line, each line fading a moment after the next
     * starts, and rings running out to the core and then the edge.
     */
    private static void hexBlast(Graphics2D g2, SpellBlastDraw blast) {
        double p = blast.progress();
        double x = blast.x();
        double y = blast.y();
        double cell = blast.scale();
        blastFlash(g2, blast, ARCANE_HOT);
        Stroke stroke = g2.getStroke();
        blastRings(blast, (r, strength) -> {
            Shape ring = new Ellipse2D.Double(x - r, y - r, r * 2, r * 2);
            stroke(g2, ring, cell * 0.1, ARCANE_GLOW, 0.35 * strength);
            stroke(g2, ring, cell * 0.03, ARCANE_HOT, 0.85 * strength);
        });
        double turn = BlastArt.hash(blast.seed(), 0) * 2 * Math.PI;
        double r = blast.core() * 0.85;
        for (int i = 0; i < PENTAGRAM_LINES; i++) {
            double start = i * PENTAGRAM_LINE_SHARE;
            double drawn = Easing.clamp01((p - start) / PENTAGRAM_LINE_SHARE);
            double gone = (p - start - PENTAGRAM_LINE_SHARE) / PENTAGRAM_LINE_LINGER;
            if (drawn <= 0 || gone >= 1) {
                continue;
            }
            double a = turn + i * 4 * Math.PI / 5;
            double b = turn + (i + 1) * 4 * Math.PI / 5;
            double fromX = x + Math.cos(a) * r;
            double fromY = y + Math.sin(a) * r;
            double toX = fromX + (x + Math.cos(b) * r - fromX) * drawn;
            double toY = fromY + (y + Math.sin(b) * r - fromY) * drawn;
            Shape line = new Line2D.Double(fromX, fromY, toX, toY);
            double strength = 1 - Easing.clamp01(gone);
            stroke(g2, line, cell * 0.12, ARCANE_GLOW, 0.45 * strength);
            stroke(g2, line, cell * 0.04, ARCANE_HOT, strength);
        }
        g2.setStroke(stroke);
    }

    /** A circle of {@code r} round {@code x, y} whose points swing in and out by up to {@code jitter}. */
    private static Path2D.Double jaggedRing(double x, double y, double r, double jitter, int seed) {
        int points = Math.max(16, (int) Math.ceil(r * 1.2));
        Path2D.Double ring = new Path2D.Double();
        for (int i = 0; i < points; i++) {
            double a = i * Math.PI * 2 / points;
            double rr = r + (BlastArt.hash(seed, i) - 0.5) * 2 * jitter;
            if (i == 0) {
                ring.moveTo(x + Math.cos(a) * rr, y + Math.sin(a) * rr);
            } else {
                ring.lineTo(x + Math.cos(a) * rr, y + Math.sin(a) * rr);
            }
        }
        ring.closePath();
        return ring;
    }

    /** A jagged line of {@code joints} pieces from one point to another, each joint swinging up to {@code swing}. */
    private static Path2D.Double zigzag(double fromX, double fromY, double toX, double toY, int joints, double swing,
                                        int seed) {
        double dx = toX - fromX;
        double dy = toY - fromY;
        double length = Math.hypot(dx, dy);
        Path2D.Double path = new Path2D.Double();
        path.moveTo(fromX, fromY);
        if (length > 0) {
            double nx = -dy / length;
            double ny = dx / length;
            for (int i = 1; i < joints; i++) {
                double t = (double) i / joints;
                double off = (BlastArt.hash(seed, i) - 0.5) * 2 * swing;
                path.lineTo(fromX + dx * t + nx * off, fromY + dy * t + ny * off);
            }
        }
        path.lineTo(toX, toY);
        return path;
    }
}
