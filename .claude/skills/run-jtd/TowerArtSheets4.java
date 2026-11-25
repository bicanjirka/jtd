import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.Set;
import td.effect.DamageSink;
import td.enemy.EnemyMob;
import td.zone.Zone;
import td.zone.ZoneKind;

/** Enemy marks, grounds, icons and panel thumbnails. Throwaway lab code. */
final class TowerArtSheets4 {

    static final Color SILENCE = new Color(170, 120, 255);
    static final Color TOLL = new Color(255, 170, 70);
    static final Color DOOM = new Color(190, 90, 255);
    static final Color BLIGHT = new Color(140, 200, 60);
    static final Color BACKLASH = new Color(255, 70, 120);

    private TowerArtSheets4() {
    }

    static void run(Set<String> only) throws Exception {
        if (only.isEmpty() || only.contains("marks")) {
            marks();
        }
        if (only.isEmpty() || only.contains("grounds")) {
            grounds();
        }
        if (only.isEmpty() || only.contains("icons")) {
            icons();
        }
        TowerArtSheets5.run(only);
    }

    // ---- enemy marks -----------------------------------------------------------------------------------------

    /** Silence: a small struck-out speech mark over the head. */
    static void silence(Graphics2D g) {
        Shape bubble = new Area(new java.awt.geom.RoundRectangle2D.Double(-0.11, -0.09, 0.22, 0.15, 0.08, 0.08));
        ((Area) bubble).add(new Area(TowerArt.poly(-0.05, 0.05, -0.01, 0.05, -0.06, 0.11)));
        g.setColor(new Color(0x0d0e10));
        TowerArtSheets3.stroke(g, 0.04);
        g.draw(bubble);
        g.setColor(SILENCE);
        g.fill(bubble);
        g.setColor(new Color(0x0d0e10));
        TowerArtSheets3.stroke(g, 0.035);
        g.draw(TowerArt.line(-0.075, 0.035, 0.075, -0.075));
    }

    /** Toll: five segments round the enemy fill as stacks land; a ripple runs out on each stack gained. */
    static void toll(Graphics2D g, int stacks, boolean ripple) {
        double r = 0.33;
        for (int i = 0; i < 5; i++) {
            g.setColor(i < stacks ? TOLL : TowerArt.alpha(TOLL, 0.22));
            g.setStroke(new BasicStroke(stacks == 5 ? 0.05f : 0.036f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
            g.draw(new java.awt.geom.Arc2D.Double(-r, -r, r * 2, r * 2, 90 - i * 72 - 6, -60, java.awt.geom.Arc2D.OPEN));
        }
        if (ripple) {
            g.setColor(TowerArt.alpha(TOLL, 0.5));
            TowerArtSheets3.stroke(g, 0.018);
            g.draw(TowerArt.circ(0, 0, 0.47));
        }
    }

    /** A hex worn as a curse: three arcs of a broken rune circle in the hex's colour, its sign over the head. */
    static void hex(Graphics2D g, Color c, String sign, double weight) {
        double r = 0.34;
        if (weight > 1) {
            g.setColor(TowerArt.alpha(c, 0.14 * weight));
            g.fill(TowerArt.circ(0, 0, r));
        }
        g.setColor(TowerArt.alpha(c, 0.9));
        g.setStroke(new BasicStroke((float) (0.022 * weight), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 3; i++) {
            g.draw(new java.awt.geom.Arc2D.Double(-r, -r, r * 2, r * 2, 100 + i * 120, 80, java.awt.geom.Arc2D.OPEN));
            double a = -(100 + i * 120) - 90;
            g.draw(TowerArt.line(TowerArt.cos(a) * (r - 0.04), TowerArt.sin(a) * (r - 0.04), TowerArt.cos(a) * (r + 0.04), TowerArt.sin(a) * (r + 0.04)));
        }
        AffineTransform save = g.getTransform();
        g.translate(0, -0.47);
        g.setColor(c);
        switch (sign) {
            case "doom" -> g.fill(TowerArt.poly(-0.07, -0.05, 0.07, -0.05, 0, 0.06));
            case "blight" -> {
                g.fill(TowerArt.circ(0, 0.015, 0.05));
                g.fill(TowerArt.poly(-0.043, -0.005, 0, -0.085, 0.043, -0.005));
            }
            default -> {
                g.fill(TowerArt.poly(-0.075, -0.012, -0.03, -0.075, 0.015, -0.012));
                g.fill(TowerArt.poly(-0.015, 0.012, 0.03, 0.075, 0.075, 0.012));
            }
        }
        g.setTransform(save);
    }

    /** Today's freeze block in another colour: the same shape, facets and rim. */
    static void crystal(Graphics2D g, Color top, Color middle, Color bottom, Color rim, double fill) {
        double r = 0.36;
        Shape block = TowerArt.poly(-0.2 * r / 0.36, -r, 0.24, -r * 0.9, r, -0.1, r * 0.8, r * 0.85, -0.1, r, -r, r * 0.5, -r * 0.95, -0.2);
        g.setPaint(new LinearGradientPaint(0f, (float) -r * 2, 0f, (float) r * 1.6f, new float[] {0f, 0.55f, 1f},
                new Color[] {TowerArt.alpha(top, 0.72 * fill), TowerArt.alpha(middle, 0.46 * fill), TowerArt.alpha(bottom, 0.62 * fill)}));
        g.fill(block);
        g.setColor(TowerArt.alpha(rim, 0.5));
        TowerArtSheets3.stroke(g, 0.012);
        for (double[] p : new double[][] {{-0.2, -r}, {r, -0.1}, {-0.1, r}, {-r * 0.95, -0.2}}) {
            g.draw(TowerArt.line(-0.07, -0.11, p[0], p[1]));
        }
        g.setColor(TowerArt.alpha(rim, 0.95));
        TowerArtSheets3.stroke(g, 0.02);
        g.draw(block);
    }

    static void shards(Graphics2D g, Color c, Color edge) {
        for (int i = 0; i < 8; i++) {
            Shape s = TowerArt.at(TowerArt.poly(-0.05, -0.025, 0.07, 0, -0.05, 0.025), i * 45 + 12, 0.36 + (i % 3) * 0.05);
            g.setColor(c);
            g.fill(s);
            g.setColor(edge);
            TowerArtSheets3.stroke(g, 0.01);
            g.draw(s);
        }
    }

    static void marks() throws Exception {
        double zoom = 3;
        String[] names = {"Glare|a glint on an|Exposed enemy", "Shade thaw|dark shards", "Shade freeze|smoky dark, pale|green edge", "Ruby thaw|red shards",
            "Ruby freeze|today's block in|ruby red", "Freeze|today's, unchanged", "Backlash", "Blight, grown|thicker with the slow", "Blight|a fresh one",
            "Doom|a hex worn as a|curse", "Toll, full|5 of 5, bolder", "Toll, 3 stacks", "Toll, 1 stack|one ripple per|stack gained",
            "Silence|a mark over the|head, new"};
        String[] walkers = new String[names.length];
        java.util.Arrays.fill(walkers, "walker");
        walkers[4] = "knight";
        walkers[9] = "knight";
        TowerArtSheets2.level(walkers);
        TowerArtSheets2.tick(8);
        EnemyMob[] mobs = TowerArtSheets2.enemies();
        System.out.println(EffectCommand.apply("FREEZE 400", new EnemyMob[] {mobs[5]}));
        TowerArtSheets2.tick(2);
        BufferedImage ground = TowerArtSheets2.ground(zoom);
        Graphics2D g = TowerArtSheets.pen(ground);
        double px = 32 * zoom;
        for (int i = 0; i < mobs.length; i++) {
            int index = i;
            TowerArtSheets.cell(g, mobs[i].getX() * zoom, mobs[i].getY() * zoom, px, () -> {
                switch (index) {
                    case 0 -> {
                        g.setColor(Color.WHITE);
                        g.fill(TowerArt.star(0.16, -0.16, 4, 0.2, 0.035, 0));
                    }
                    case 1 -> shards(g, new Color(0x2a2e3a), TowerArt.col(TowerArt.SHADE));
                    case 2 -> crystal(g, new Color(96, 108, 118), new Color(30, 34, 44), new Color(12, 14, 22), TowerArt.col(TowerArt.SHADE), 1.35);
                    case 3 -> shards(g, TowerArt.col(TowerArt.RUBY), new Color(0xffd6de));
                    case 4 -> crystal(g, new Color(255, 214, 222), new Color(224, 72, 104), new Color(150, 24, 60), new Color(0xffeef2), 1.0);
                    case 6 -> hex(g, BACKLASH, "backlash", 1);
                    case 7 -> hex(g, BLIGHT, "blight", 2.2);
                    case 8 -> hex(g, BLIGHT, "blight", 1);
                    case 9 -> hex(g, DOOM, "doom", 1);
                    case 10 -> toll(g, 5, false);
                    case 11 -> toll(g, 3, false);
                    case 12 -> toll(g, 1, true);
                    case 13 -> {
                        g.translate(0, -0.46);
                        silence(g);
                    }
                    default -> {
                    }
                }
            });
        }
        g.dispose();
        double left = Math.max(0, mobs[mobs.length - 1].getX() / 32 - 0.8);
        double right = Math.min(17.9, mobs[0].getX() / 32 + 0.8);
        BufferedImage strip = TowerArtSheets2.crop(ground, zoom, left, 0.25, right, 1.75);
        BufferedImage img = TowerArtSheets.image(strip.getWidth() + 40, strip.getHeight() + 120, TowerArtSheets.PAPER);
        Graphics2D p = TowerArtSheets.pen(img);
        TowerArtSheets.title(p, "Enemy marks the new towers need (3x), on the game's own enemies walking Iron Crossing", 20, 26);
        p.drawImage(strip, 20, 40, null);
        for (int i = 0; i < mobs.length; i++) {
            String[] lines = names[i].split("\\|");
            double x = 20 + (mobs[i].getX() / 32 - left) * px - 40;
            for (int l = 0; l < lines.length; l++) {
                TowerArtSheets.text(p, lines[l], x, 40 + strip.getHeight() + 16 + l * 13, l == 0 ? TowerArtSheets.TEXT : TowerArtSheets.DIM, 11, false);
            }
        }
        p.dispose();
        TowerArtSheets.save(img, "marks.png");
    }

    // ---- grounds ---------------------------------------------------------------------------------------------

    /** One Obelisk field look: every field of the look as one patch, an even fill and one outline round them all. */
    static void patch(Graphics2D g, String look, double[][] stones, double r) {
        Area area = new Area();
        for (double[] s : stones) {
            area.add(new Area(TowerArt.circ(s[0], s[1], r)));
        }
        Color c = switch (look) {
            case "burden" -> new Color(0x7a6a52);
            case "gravity" -> new Color(0x4d6fb8);
            case "umbra" -> new Color(0x5a4f8a);
            default -> TowerArt.col(TowerArt.OBELISK);
        };
        g.setColor(TowerArt.alpha(c, look.equals("burden") ? 0.34 : 0.16));
        g.fill(area);
        if (look.equals("gravity")) {
            Shape clip = g.getClip();
            g.clip(area);
            for (double[] s : stones) {
                g.setPaint(new java.awt.RadialGradientPaint((float) s[0], (float) s[1], (float) (r * 0.8), new float[] {0f, 1f},
                        new Color[] {TowerArt.alpha(0x1c2c5c, 0.85), TowerArt.alpha(0x1c2c5c, 0)}));
                g.fill(TowerArt.circ(s[0], s[1], r * 0.8));
            }
            g.setClip(clip);
        }
        g.setColor(TowerArt.alpha(TowerArt.mix(c, Color.WHITE, 0.25), 0.7));
        TowerArtSheets3.stroke(g, 0.022);
        g.draw(area);
        if (look.equals("ward")) {
            Area inner = new Area();
            for (double[] s : stones) {
                inner.add(new Area(TowerArt.circ(s[0], s[1], r - 0.07)));
            }
            g.setColor(TowerArt.alpha(SILENCE, 0.85));
            g.setStroke(new BasicStroke(0.014f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f, new float[] {0.16f, 0.05f, 0.03f, 0.05f}, 0f));
            g.draw(inner);
        }
    }

    static void zone(ZoneKind kind, double cellX, double cellY, double radius) {
        TowerArtSheets2.engine.getGameWorld().zones().add(new Zone(kind, cellX * 32, cellY * 32, (float) (radius * 32), 400, 100,
                (target, effect) -> target.applyEffect(effect.apply(DamageSink.none()))));
    }

    static void grounds() throws Exception {
        double zoom = 2.5;
        TowerArtSheets2.level("walker", "walker", "knight", "walker", "walker");
        zone(ZoneKind.BURNING_GROUND, 2.0, 9.0, 0.9);
        zone(ZoneKind.TAR, 4.5, 9.0, 0.9);
        zone(ZoneKind.FROST_GROUND, 7.0, 9.0, 0.9);
        zone(ZoneKind.FALLOUT, 9.5, 9.0, 0.9);
        TowerArtSheets2.tick(60);
        BufferedImage img = TowerArtSheets2.ground(zoom);
        Graphics2D g = TowerArtSheets.pen(img);
        double px = 32 * zoom;
        String[] looks = {"plain", "ward", "burden", "gravity", "umbra"};
        String[] captions = {"Plain field: one patch for both stones", "Ward: a rune circle inside the outline", "Burden: a denser, heavier fill",
            "Gravity: darker toward each stone", "Umbra: the Sundial is darker inside the patch"};
        double[] xs = {2.0, 5.3, 8.6, 11.9, 15.2};
        for (int i = 0; i < looks.length; i++) {
            String look = looks[i];
            double x = xs[i];
            double[][] stones = {{-0.55, 0}, {0.55, 0.3}};
            TowerArtSheets.cell(g, x * px, 3.6 * px, px, () -> {
                patch(g, look, stones, 1.0);
                if (look.equals("umbra")) {
                    AffineTransform save = g.getTransform();
                    Shape clip = g.getClip();
                    g.translate(stones[1][0], stones[1][1]);
                    Area field = new Area();
                    for (double[] s : stones) {
                        field.add(new Area(TowerArt.circ(s[0] - stones[1][0], s[1] - stones[1][1], 1.0)));
                    }
                    TowerArtSheets3.sundial(g, -150, 4.0, false);
                    g.clip(field);
                    TowerArtSheets3.sundial(g, -150, 4.0, true);
                    g.setClip(clip);
                    g.setTransform(save);
                }
                for (double[] s : stones) {
                    AffineTransform save = g.getTransform();
                    g.translate(s[0], s[1]);
                    String rune = look.equals("plain") || look.equals("ward") ? "" : look;
                    TowerArt.paintCell(g, TowerArt.build("obelisk", 1, TowerArt.State.idle().look(rune)), px);
                    g.setTransform(save);
                }
            });
            TowerArtSheets.text(g, captions[i], (x - 1.55) * px, 5.25 * px, TowerArtSheets.TEXT, 11, false);
        }
        // the flare and the Ice Wake, beside the payload grounds the game already paints
        TowerArtSheets.cell(g, 12.0 * px, 9.0 * px, px, () -> {
            g.setPaint(new java.awt.RadialGradientPaint(0f, 0f, 0.9f, new float[] {0f, 0.6f, 1f},
                    new Color[] {TowerArt.alpha(0xfff2a8, 0.30), TowerArt.alpha(0xfff2a8, 0.10), TowerArt.alpha(0xfff2a8, 0)}));
            g.fill(TowerArt.circ(0, 0, 0.9));
            g.setColor(new Color(0xfffbe0));
            g.fill(TowerArt.star(0, 0, 4, 0.09, 0.025, 45));
        });
        EnemyMob[] mobs = TowerArtSheets2.enemies();
        TowerArtSheets.cell(g, 14.6 * px, 9.0 * px, px, () -> {
            g.setColor(TowerArt.alpha(0xd6f1ff, 0.10));
            g.fill(TowerArt.circ(0, 0, 0.85));
            g.setColor(TowerArt.alpha(0xd6f1ff, 0.55));
            g.setStroke(new BasicStroke(0.02f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[] {0.09f, 0.07f}, 0f));
            g.draw(TowerArt.circ(0, 0, 0.85));
            crystal(g, new Color(225, 246, 255), new Color(120, 195, 245), new Color(70, 140, 215), new Color(240, 251, 255), 1.0);
        });
        String[] low = {"Burning ground (today's)", "Tar (today's)", "Frost ground (today's)", "Fallout (today's)", "Flare: a faint, calm light",
            "Ice Wake: a frost ring round the ice block"};
        double[] lowX = {2.0, 4.5, 7.0, 9.5, 12.0, 14.6};
        for (int i = 0; i < low.length; i++) {
            TowerArtSheets.text(g, low[i], (lowX[i] - 0.95) * px, 10.25 * px, TowerArtSheets.TEXT, 11, false);
        }
        g.dispose();
        BufferedImage cut = TowerArtSheets2.crop(img, zoom, 0, 1.9, 17.95, 10.6);
        BufferedImage out = TowerArtSheets.image(cut.getWidth() + 40, cut.getHeight() + 60, TowerArtSheets.PAPER);
        Graphics2D p = TowerArtSheets.pen(out);
        TowerArtSheets.title(p, "The ground the towers leave (2.5x, Iron Crossing). Nothing here moves while it holds nobody.", 20, 26);
        p.drawImage(cut, 20, 40, null);
        p.dispose();
        TowerArtSheets.save(out, "grounds.png");
    }

    // ---- toolbar icons, the upgrade arrow, panel thumbnails -------------------------------------------------

    /** A toolbar icon in cell units, as the game paints it. */
    static void icon(Graphics2D g, String id) {
        td.ui.TowerArtGame.paintIcon(g, TowerArt.look(id, TowerArt.State.idle()));
    }

    static void icons() throws Exception {
        String[] ids = {"sniper", "burst", "mortar", "scorcher", "aerie", "obelisk", "beacon"};
        String[] names = {"Sniper", "Burst", "Mortar", "Scorcher", "Aerie", "Obelisk", "Beacon"};
        String[] keys = {"q", "w", "e", "r", "t", "y", "u"};
        BufferedImage img = TowerArtSheets.image(1180, 760, TowerArtSheets.PAPER);
        Graphics2D g = TowerArtSheets.pen(img);
        TowerArtSheets.title(g, "Toolbar icons (two colours each), the upgrade arrow, and the panel's tier thumbnails", 20, 26);
        TowerArtSheets.label(g, "The seven toolbar icons at the toolbar's 32 px, on the three tile fills (background, hover, selected), then 3x", 20, 50);
        Color[] fills = {new Color(0x0d0e10), new Color(0x29292a), new Color(0x39393a)};
        for (int i = 0; i < ids.length; i++) {
            String id = ids[i];
            for (int f = 0; f < 3; f++) {
                double x = 20 + i * 52;
                double y = 62 + f * 46;
                g.setColor(fills[f]);
                g.fill(new java.awt.geom.Rectangle2D.Double(x, y, 40, 40));
                g.setColor(new Color(0x676769));
                g.setStroke(new BasicStroke(1f));
                g.draw(new java.awt.geom.Rectangle2D.Double(x + 0.5, y + 0.5, 39, 39));
                TowerArtSheets.cell(g, x + 20, y + 20, 32, () -> icon(g, id));
            }
            double x = 420 + i * 106;
            g.setColor(fills[0]);
            g.fill(new java.awt.geom.Rectangle2D.Double(x, 62, 100, 100));
            TowerArtSheets.cell(g, x + 50, 112, 96, () -> icon(g, id));
            TowerArtSheets.text(g, names[i] + "  [" + keys[i] + "]", x, 178, TowerArtSheets.DIM, 11, false);
        }
        double y = 226;
        TowerArtSheets.label(g, "The one upgrade mark: a green up arrow in the cell's top-left corner (the Jammed marker keeps the top-right). 1x, 0.5x, 3x", 20, y);
        double[] zooms = {1, 0.5, 3};
        double x = 20;
        for (double zoom : zooms) {
            double px = 32 * zoom;
            TowerArtSheets.tile(g, x, y + 10, px, px);
            TowerArtSheets.tower(g, "mortar", 2, TowerArt.State.idle(), x + px / 2, y + 10 + px / 2, px);
            TowerArtSheets.cell(g, x + px * 0.16, y + 10 + px * 0.16, px, () -> TowerArt.upgradeArrow(g));
            x += px + 14;
        }
        TowerArtSheets.label(g, "Lit by a selected Beacon: a small gold lantern mark in the bottom-left corner. 1x, 3x", 420, y + 24);
        double lx = 420;
        for (double zoom : new double[] {1, 3}) {
            double px = 32 * zoom;
            TowerArtSheets.tile(g, lx, y + 34, px, px);
            TowerArtSheets.tower(g, "sniper", 1, TowerArt.State.idle(), lx + px / 2, y + 34 + px / 2, px);
            TowerArtSheets.cell(g, lx + px * 0.14, y + 34 + px * 0.86, px, () -> {
                g.setColor(TowerArt.PAPER_GAP);
                g.fill(TowerArt.ngon(0, 0, 4, 0.095, 0));
                g.setColor(TowerArt.col(0xffe9a8));
                g.fill(TowerArt.ngon(0, 0, 4, 0.065, 0));
            });
            lx += px + 14;
        }
        y += 140;
        TowerArtSheets.label(g, "Upgrades panel, one row per tier: the tier's foundation as a thumbnail and the tier colour. "
                + "A locked tier's thumbnail is grey. Shown: every tower with Tiers 0 and I open, II and III locked.", 20, y);
        y += 14;
        int[] tierRow = {0, 1, 4, 5, 6, 7, 8};
        for (int i = 0; i < ids.length; i++) {
            double cx = 20 + i * 164;
            TowerArtSheets.text(g, names[i], cx, y + 14, TowerArtSheets.TEXT, 12, true);
            for (int t = 0; t < 4; t++) {
                double ry = y + 22 + t * 40;
                Color tier = TowerArt.col(TowerArt.TIERS[i == 1 && t > 0 ? 2 : tierRow[i]][t]);
                boolean locked = t >= 2;
                g.setColor(new Color(0x171717));
                g.fill(new java.awt.geom.Rectangle2D.Double(cx, ry, 150, 34));
                g.setColor(locked ? new Color(0x3e3e3f) : tier);
                g.fill(new java.awt.geom.Rectangle2D.Double(cx, ry, 3, 34));
                java.awt.geom.AffineTransform pen = g.getTransform();
                g.translate((int) cx + 25, (int) ry + 23);
                td.ui.TowerArtGame.paintThumbnail(g, TowerArt.look(ids[i], TowerArt.State.idle()), t, 25, locked);
                g.setTransform(pen);
                TowerArtSheets.text(g, TowerArtSheets.TIER_NAMES[t], cx + 48, ry + 15, locked ? new Color(0x717173) : tier, 12, true);
                TowerArtSheets.text(g, locked ? "locked" : String.format("#%06x", tier.getRGB() & 0xffffff), cx + 48, ry + 28,
                        TowerArtSheets.GLOSS, 10, false);
            }
        }
        y += 22 + 4 * 40 + 30;
        TowerArtSheets.label(g, "Tier colours, Tier 0 to III, for the panel rows and the unlock ring (the Burst's forks have their own from Tier I)", 20, y);
        String[] ramps = {"Sniper", "Burst", "Stormcaller", "Hexer", "Mortar", "Scorcher", "Aerie", "Obelisk", "Beacon"};
        for (int i = 0; i < ramps.length; i++) {
            double cx = 20 + i * 127;
            TowerArtSheets.text(g, ramps[i], cx, y + 24, TowerArtSheets.TEXT, 11, false);
            for (int t = i == 2 || i == 3 ? 1 : 0; t < (i == 1 ? 1 : 4); t++) {
                g.setColor(TowerArt.col(TowerArt.TIERS[i][t]));
                g.fill(new java.awt.geom.Rectangle2D.Double(cx + t * 28, y + 30, 26, 26));
                TowerArtSheets.text(g, String.format("%06x", TowerArt.TIERS[i][t]), cx + t * 28 - 1, y + 68 + (t % 2) * 11, TowerArtSheets.GLOSS, 8, false);
            }
        }
        g.dispose();
        TowerArtSheets.save(img.getSubimage(0, 0, img.getWidth(), (int) (y + 96)), "icons.png");
    }
}
