import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.Set;

/** Heads and their cues, and the shots and casts in flight. Throwaway lab code. */
final class TowerArtSheets3 {

    static final int TILE = 118;
    static final double PX = 80;

    private TowerArtSheets3() {
    }

    static void run(Set<String> only) throws Exception {
        if (only.isEmpty() || only.contains("heads")) {
            heads();
        }
        if (only.isEmpty() || only.contains("shots")) {
            shots();
        }
        TowerArtSheets4.run(only);
    }

    interface Art {
        void draw(Graphics2D g);
    }

    /** One captioned tile: {@code art} draws in cell units around the tile's centre, clipped to the tile. */
    static void tile(Graphics2D g, double x, double y, double w, double h, double px, String caption, Art art) {
        TowerArtSheets.tile(g, x, y, w, h);
        Shape clip = g.getClip();
        g.clip(new java.awt.geom.Rectangle2D.Double(x + 1, y + 1, w - 2, h - 2));
        AffineTransform save = g.getTransform();
        g.translate(x + w / 2, y + h / 2);
        g.scale(px, px);
        art.draw(g);
        g.setTransform(save);
        g.setClip(clip);
        String[] lines = caption.split("\\|");
        for (int i = 0; i < lines.length; i++) {
            TowerArtSheets.text(g, lines[i], x + 2, y + h + 13 + i * 13, i == 0 ? TowerArtSheets.TEXT : TowerArtSheets.DIM, 11, false);
        }
    }

    static Art tower(String id, int tier, TowerArt.State s) {
        return g -> {
            g.translate(0, 0.24);
            g.scale(1 / PX, 1 / PX);
            TowerArtSheets.tower(g, id, tier, s, 0, 0, PX);
        };
    }

    static TowerArt.State st() {
        return TowerArt.State.idle();
    }

    /** A Scorcher with its stream, the nozzle at the tile's left and drawn small, so the flame's tips past its range fit. */
    static Art streaming(String id, int tier, TowerArt.State s, double range, double half, String look) {
        return g -> {
            g.translate(-1.0, 0.2);
            g.scale(0.75, 0.75);
            TowerArt.Build b = TowerArt.build(id, tier, s.aim(0).cue("stream"));
            AffineTransform save = g.getTransform();
            double len = (s.has("dragon") ? 0.28 : 0.25) + 0.07;
            g.translate(len, TowerArt.LIFT * b.top());
            float core = look.contains("core") ? (float) Math.toRadians(half) * 0.25f : 0f;
            td.ui.TowerArtGame.paintStream(g, range, half, new td.tower.CinderTower.StreamLook(look.contains("soul"),
                    look.contains("white"), s.has("dragon"), core), 11);
            g.setTransform(save);
            TowerArt.paintCell(g, b, PX);
        };
    }

    static double row(Graphics2D g, double y, String name, double[] widths, String[] captions, Art... arts) {
        TowerArtSheets.text(g, name, 20, y + 14, TowerArtSheets.TEXT, 13, true);
        double x = 20;
        for (int i = 0; i < arts.length; i++) {
            double w = widths == null ? TILE : widths[i];
            tile(g, x, y + 24, w, TILE, PX, captions[i], arts[i]);
            x += w + 10;
        }
        return y + 24 + TILE + 46;
    }

    static void heads() throws Exception {
        BufferedImage img = TowerArtSheets.image(1470, 1960, TowerArtSheets.PAPER);
        Graphics2D g = TowerArtSheets.pen(img);
        TowerArtSheets.title(g, "Moving parts and their cues, raised view (2.5x). Idle is still; every other tile is a moment that game state causes.", 20, 26);
        double y = 44;
        y = row(g, y, "Sniper: the rifle", null, new String[] {"Idle, ready", "Fired|recoil, muzzle flash", "Steady Aim 1|the lens lights", "Steady Aim 2",
            "Steady Aim full|the lens sparkles", "Fourth Shot primed|white muzzle", "Silver Round|silver tip", "Hollow Point|orange band",
            "All three primed", "Frenzy|shake, red sparks"},
                tower("sniper", 2, st()), tower("sniper", 2, st().fire(1)), tower("sniper", 2, st().stacks(1)),
                tower("sniper", 2, st().stacks(2)), tower("sniper", 2, st().stacks(3)), tower("sniper", 2, st().cue("fourth")),
                tower("sniper", 2, st().cue("silver")), tower("sniper", 2, st().cue("hollow")),
                tower("sniper", 2, st().cue("fourth", "silver", "hollow")), tower("sniper", 2, st().cue("frenzy")));
        y = row(g, y, "Sniper, continued; Burst: the arm, the crown, the brew (no turning head)", null,
                new String[] {"Headhunter|aiming at Elite or Boss", "Long Barrel|a range node", "Thrower: loaded", "Thrower: thrown|the arm flicks",
                    "Thrower: reloading|arm back, no stone yet", "Stormcaller: idle", "Stormcaller: thrown|the crown sparks once",
                    "Stormcaller: ability full|steady glow", "Hexer: idle", "Hexer: cast|the brew bubbles up"},
                tower("sniper", 2, st().cue("headhunter")), tower("sniper", 2, st().cue("long")), tower("burst", 0, st()),
                tower("burst", 0, st().fire(1)), tower("burst", 0, st().reload(0.2)), tower("stormcaller", 2, st()),
                tower("stormcaller", 2, st().fire(1)), tower("stormcaller", 2, st().cue("full")), tower("hexer", 2, st()),
                tower("hexer", 2, st().fire(1)));
        y = row(g, y, "Hexer, continued; Mortar: the barrel and the shell in it", null,
                new String[] {"Hexer: ability full|the brew glows", "Mortar: cocked|pumped, needle in the red", "Spit|stretched up, bore empty", "Slam|squashed onto its knees",
                    "Heavy|larger, bright band", "Nuke|pale, hazard glow", "Napalm", "Tar|black, glossy", "Cryo", "Flare"},
                tower("hexer", 2, st().cue("full")), tower("mortar", 2, st().beat(10)), tower("mortar", 2, st().fire(1).beat(1)),
                tower("mortar", 2, st().reload(0.05).beat(2)), tower("mortar", 2, st().shell("heavy")), tower("mortar", 2, st().shell("nuke")),
                tower("mortar", 2, st().shell("napalm")), tower("mortar", 2, st().shell("tar")), tower("mortar", 2, st().shell("cryo")),
                tower("mortar", 2, st().shell("flare")));
        double[] wide = {TILE, TILE, TILE, 250, 250, 250, 250};
        y = row(g, y, "Mortar, continued; Scorcher: the nozzle", wide,
                new String[] {"Volley|three shells on the tray", "Scorcher: idle|pilot flame, cold nozzle", "Just stopped|red nozzle, cooling",
                    "Streaming|cone 60 degrees, hottest at the nozzle", "Core|a brighter band down the middle", "White Flame",
                    "Dragon's Breath|thin and long"},
                tower("mortar", 2, st().cue("volley")), tower("scorcher", 2, st()), tower("scorcher", 2, st().reload(0.0)),
                streaming("scorcher", 2, st(), 2.0, 30, ""), streaming("scorcher", 2, st(), 2.0, 30, "core"),
                streaming("scorcher", 2, st(), 2.0, 30, "white"), streaming("scorcher", 2, st().cue("dragon"), 3.0, 11, "core"));
        double[] wide2 = {250, 250, TILE, TILE, TILE, TILE, TILE};
        y = row(g, y, "Scorcher: Soulfire; Aerie: the nest (the stored missiles themselves)", wide2,
                new String[] {"Soulfire streaming|violet flame, pale core", "Furnace (Tier III) streaming|the vents glow", "Aerie: one missile",
                    "Two missiles|evenly spaced", "Release ready|the tip turns white", "Swarm|seven on the ring", "Ruby: Release ready"},
                streaming("soulfire", 2, st(), 2.0, 30, "soul"), streaming("scorcher", 3, st(), 2.0, 30, ""),
                tower("aerie", 1, st()), tower("aerie", 2, st().stacks(2)), tower("aerie", 2, st().stacks(2).cue("release")),
                tower("aerie", 3, st().cue("swarm")), tower("ruby", 3, st().stacks(3).cue("release")));
        y = row(g, y, "Aerie: Shade; Obelisk: the rune (the only part that glows)", null,
                new String[] {"Shade|wisps, green crown", "Plain rune|in its tier colour", "Burden|a bar over a chevron", "Gravity|a spiral drawn inward",
                    "Umbra|an eclipsed disc", "Full Toll inside|brighter, not bigger", "Knell|the stone is thrown, runes white", "Beacon: steady glow",
                    "Kinship or Prism|a pane per Lit type", "Daylight|a whiter glow"},
                tower("shade", 3, st().stacks(3)), tower("obelisk", 1, st()), tower("obelisk", 1, st().look("burden")),
                tower("obelisk", 1, st().look("gravity")), tower("obelisk", 1, st().look("umbra")),
                tower("obelisk", 1, st().look("gravity").cue("full")), tower("obelisk", 1, st().look("burden").fire(1)),
                tower("beacon", 2, st()), tower("beacon", 2, st().panes(TowerArt.SNIPER, TowerArt.MORTAR, TowerArt.SCORCHER, TowerArt.AERIE,
                        TowerArt.OBELISK, TowerArt.BURST)), tower("beacon", 2, st().cue("daylight")));
        y = unlock(g, y);
        g.dispose();
        TowerArtSheets.save(img.getSubimage(0, 0, img.getWidth(), (int) y), "heads.png");
    }

    /** The game's own moving pictures, frame by frame: a tier's unlock, a tower settling onto its cell, an idle tower's lights. */
    static double unlock(Graphics2D g, double y) {
        double[] times = {0, 0.2, 0.4, 0.6, 0.8, 1.0};
        String[] captions = {"Tier I|before the unlock", "0.1 s|a flash, pushing out", "0.2 s|swelling; one ring",
            "0.3 s|sparks fly out", "0.4 s|the ring fades", "0.5 s|Tier II, still again"};
        Art[] arts = new Art[times.length];
        for (int i = 0; i < times.length; i++) {
            double t = times[i];
            arts[i] = g2 -> {
                g2.translate(0, 0.2);
                td.ui.TowerArtGame.paintLive(g2, td.ui.render.TowerLook.MORTAR, 1, t == 0 ? 1 : 2, td.ui.render.TowerPose.idle(), -35,
                        t == 0 ? 1 : t, 1, 0);
            };
        }
        y = row(g, y, "The tier unlock, on the tick clock: the Mortar from Tier I to Tier II", null, captions, arts);
        double[] settle = {0.05, 0.25, 0.45, 0.65, 0.85, 1.0};
        String[] settling = {"Placed|it starts small and faint", "0.09 s|growing out of its cell", "0.16 s|a ring of dust runs out", "0.23 s|a little past its size",
            "0.30 s|settling back", "0.35 s|standing"};
        Art[] placed = new Art[settle.length];
        for (int i = 0; i < settle.length; i++) {
            double t = settle[i];
            placed[i] = g2 -> {
                g2.translate(0, 0.2);
                td.ui.TowerArtGame.paintLive(g2, td.ui.render.TowerLook.SNIPER, 0, 0, td.ui.render.TowerPose.idle(), -35, 1, t, 0);
            };
        }
        y = row(g, y, "A tower placed, on the tick clock: it settles onto its cell", null, captions(settling), placed);
        Object[][] alive = {{"sniper", 2, 0.22, "Sniper|the lens glints"}, {"mortar", 2, 0.22, "Mortar|the shell glints"},
            {"scorcher", 2, 0.3, "Scorcher|a flickering pilot"}, {"stormcaller", 2, 0.85, "Stormcaller|the ball breathes"},
            {"hexer", 2, 0.85, "Hexer|the brew breathes"}, {"aerie", 2, 0.85, "Aerie|its tip breathes"},
            {"obelisk", 1, 0.85, "Obelisk|the rune breathes"}, {"beacon", 2, 0.5, "Beacon|a turning ray"},
            {"beacon", 2, 2.2, "Beacon|a quarter turn later"}, {"obelisk", 1, 2.55, "Obelisk|the breath at its lowest"}};
        Art[] lights = new Art[alive.length];
        String[] names = new String[alive.length];
        for (int i = 0; i < alive.length; i++) {
            Object[] a = alive[i];
            names[i] = (String) a[3];
            lights[i] = g2 -> {
                g2.translate(0, 0.24);
                td.ui.render.TowerLook look = TowerArt.look((String) a[0], st());
                td.ui.TowerArtGame.paintLive(g2, look, (int) a[1], (int) a[1], td.ui.render.TowerPose.idle(), -35, 1, 1, (double) a[2]);
            };
        }
        return row(g, y, "Idle life, on the wall clock: the one part of a still tower that moves", null, names, lights);
    }

    static String[] captions(String[] lines) {
        return lines;
    }

    // ---- shots and casts -----------------------------------------------------------------------------------

    static void stroke(Graphics2D g, double w) {
        g.setStroke(new BasicStroke((float) w, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    }

    /** A stand-in enemy for sheets that are about the shot, not the enemy. */
    static void dummy(Graphics2D g, double x, double y) {
        g.setColor(new Color(0x227078));
        g.fill(TowerArt.circ(x, y + 0.055, 0.2));
        g.setColor(new Color(0x8fe0ea));
        g.fill(TowerArt.circ(x - 0.015, y - 0.015, 0.2));
        g.setColor(new Color(0x3ec0d6));
        g.fill(TowerArt.circ(x, y, 0.2));
        g.setColor(new Color(0xd8f6ff));
        g.fill(TowerArt.circ(x, y, 0.08));
    }

    /** The Sniper's tracer {@code t} of the way from muzzle to enemy: slim, white at its head, the tail fading. */
    static void tracer(Graphics2D g, double x0, double y0, double x1, double y1, double t, Color tail, Color head) {
        double from = Math.max(0, t - 0.45);
        int steps = 6;
        for (int i = 0; i < steps; i++) {
            double a = from + (t - from) * i / steps;
            double b = from + (t - from) * (i + 1) / steps;
            g.setColor(TowerArt.alpha(TowerArt.mix(tail, head, (double) i / (steps - 1)), 0.25 + 0.75 * i / (steps - 1)));
            stroke(g, 0.022 + 0.012 * i / steps);
            g.draw(TowerArt.line(x0 + (x1 - x0) * a, y0 + (y1 - y0) * a, x0 + (x1 - x0) * b, y0 + (y1 - y0) * b));
        }
    }

    interface Flyer {
        void draw(Graphics2D g, double x, double y, double headingDeg);
    }

    /** A lobbed shot shown at {@code ghosts} moments: its arc, the shadow under it and the thing itself. */
    static void lob(Graphics2D g, double x0, double y0, double x1, double y1, double height, int ghosts, Flyer flyer) {
        g.setColor(TowerArt.alpha(0xa9a9ac, 0.35));
        g.setStroke(new BasicStroke(0.012f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f, new float[] {0.04f, 0.05f}, 0f));
        Path2D.Double arc = new Path2D.Double();
        for (int i = 0; i <= 30; i++) {
            double t = i / 30.0;
            double x = x0 + (x1 - x0) * t;
            double y = y0 + (y1 - y0) * t - height * 4 * t * (1 - t);
            if (i == 0) {
                arc.moveTo(x, y);
            } else {
                arc.lineTo(x, y);
            }
        }
        g.draw(arc);
        for (int i = 1; i <= ghosts; i++) {
            double t = (double) i / (ghosts + 1);
            double x = x0 + (x1 - x0) * t;
            double ground = y0 + (y1 - y0) * t;
            double lift = height * 4 * t * (1 - t);
            double spread = 1 - 0.45 * lift / height;
            g.setColor(TowerArt.alpha(0x6a7084, 0.8));
            g.fill(TowerArt.ell(x, ground, 0.09 * spread, 0.05 * spread));
            double slope = (y1 - y0) - height * 4 * (1 - 2 * t);
            AffineTransform save = g.getTransform();
            flyer.draw(g, x, ground - lift, Math.toDegrees(Math.atan2(slope, x1 - x0)));
            g.setTransform(save);
        }
    }

    /** A blast's zones: the full-damage core as a white ring, the half-damage edge fainter. */
    static void blast(Graphics2D g, double x, double y, double core, double edge, double middle) {
        g.setColor(TowerArt.alpha(Color.WHITE, 0.9));
        stroke(g, 0.03);
        g.draw(TowerArt.circ(x, y, core));
        g.setColor(TowerArt.alpha(Color.WHITE, 0.3));
        stroke(g, 0.02);
        g.draw(TowerArt.circ(x, y, edge));
        if (middle > 0) {
            g.setColor(TowerArt.alpha(0xc8f04a, 0.6));
            g.draw(TowerArt.circ(x, y, middle));
        }
    }

    static Flyer shellFlyer(String kind) {
        return (g, x, y, heading) -> {
            Color trail = switch (kind) {
                case "napalm" -> new Color(0xff8a2a);
                case "cryo" -> new Color(0xd6f1ff);
                case "flare" -> new Color(0xfff2a8);
                default -> null;
            };
            if (trail != null) {
                for (int i = 1; i <= 4; i++) {
                    g.setColor(TowerArt.alpha(trail, 0.6 - i * 0.12));
                    g.fill(TowerArt.circ(x - TowerArt.cos(heading) * i * 0.055, y - TowerArt.sin(heading) * i * 0.055, 0.034 - i * 0.005));
                }
            }
            AffineTransform save = g.getTransform();
            g.translate(x, y);
            td.ui.TowerArtGame.paintShell(g, td.ui.render.LoadedShell.valueOf(kind.toUpperCase()), 0.06, 64);
            g.setTransform(save);
        };
    }

    static Flyer stone(int rgb, boolean charged) {
        return (g, x, y, heading) -> {
            if (charged) {
                g.setPaint(new java.awt.RadialGradientPaint((float) x, (float) y, 0.12f, new float[] {0f, 1f},
                        new Color[] {TowerArt.alpha(rgb, 0.8), TowerArt.alpha(rgb, 0)}));
                g.fill(TowerArt.circ(x, y, 0.12));
            }
            td.ui.TowerArtGame.paintSolid(g, TowerArt.ngon(x, y, 6, 0.07, 10), 0.02, TowerArt.col(rgb), 64);
        };
    }

    static Shape bolt(double x0, double y0, double x1, double y1, int kinks, double sway, int seed) {
        Random rnd = new Random(seed);
        Path2D.Double p = new Path2D.Double();
        p.moveTo(x0, y0);
        double nx = -(y1 - y0);
        double ny = x1 - x0;
        double len = Math.hypot(nx, ny);
        for (int i = 1; i < kinks; i++) {
            double t = (double) i / kinks;
            double off = (rnd.nextDouble() - 0.5) * 2 * sway;
            p.lineTo(x0 + (x1 - x0) * t + nx / len * off, y0 + (y1 - y0) * t + ny / len * off);
        }
        p.lineTo(x1, y1);
        return p;
    }

    static void lightning(Graphics2D g, Shape bolt, int rgb, double width) {
        g.setColor(TowerArt.alpha(rgb, 0.45));
        stroke(g, width * 2.6);
        g.draw(bolt);
        g.setColor(new Color(0xf0f4ff));
        stroke(g, width);
        g.draw(bolt);
    }

    /** A hex in flight heading +x from 0,0: a dark-hearted bolt in the hex's marker colour, a wisp behind it. */
    static void curse(Graphics2D g, double x, double y, double heading, Color c) {
        AffineTransform save = g.getTransform();
        g.translate(x, y);
        g.rotate(Math.toRadians(heading));
        g.scale(1.8, 1.8);
        for (int i = 1; i <= 4; i++) {
            g.setColor(TowerArt.alpha(c, 0.5 - i * 0.1));
            g.fill(TowerArt.circ(-0.07 * i, (i % 2 == 0 ? 1 : -1) * 0.018, 0.04 - i * 0.006));
        }
        g.setColor(c);
        g.fill(TowerArt.poly(0.08, 0, 0.0, 0.05, -0.07, 0, 0.0, -0.05));
        g.setColor(new Color(0x14101c));
        g.fill(TowerArt.circ(0.005, 0, 0.022));
        g.setTransform(save);
    }

    static void shots() throws Exception {
        double px = 64;
        BufferedImage img = TowerArtSheets.image(1320, 1500, TowerArtSheets.PAPER);
        Graphics2D g = TowerArtSheets.pen(img);
        TowerArtSheets.title(g, "Shots and casts in flight (2x). Dotted arcs and ghosts show travel; the game draws one position a frame.", 20, 26);
        double y = 40;
        double w = 310;
        double h = 150;
        Color green = TowerArt.col(TowerArt.SNIPER);
        // Sniper
        TowerArtSheets.text(g, "Sniper", 20, y + 14, TowerArtSheets.TEXT, 13, true);
        tile(g, 20, y + 22, w, h, px, "Tracer, 3 frames from muzzle to enemy|slim, white head, green tail", gg -> {
            sniperScene(gg);
            for (int i = 1; i <= 3; i++) {
                tracer(gg, -1.55, 0.72 - i * 0.22 + 0.22, 1.7, -0.75 - i * 0.22 + 0.44 + 0.22, i / 3.0, green, Color.WHITE);
            }
        });
        tile(g, 340, y + 22, w, h, px, "Crit: the bullet stays white|Silver Round: its own silver line", gg -> {
            sniperScene(gg);
            tracer(gg, -1.55, 0.5, 1.7, -0.53, 0.75, Color.WHITE, Color.WHITE);
            tracer(gg, -1.55, 0.72, 1.7, -0.31, 0.6, new Color(0xaad2ff), Color.WHITE);
        });
        tile(g, 660, y + 22, w, h, px, "Fourth Shot: a wider white bolt|Hollow Point: an orange tail", gg -> {
            sniperScene(gg);
            gg.setColor(TowerArt.alpha(Color.WHITE, 0.35));
            stroke(gg, 0.09);
            gg.draw(TowerArt.line(-0.4, 0.14, 0.9, -0.28));
            tracer(gg, -1.55, 0.5, 1.7, -0.53, 0.78, Color.WHITE, Color.WHITE);
            tracer(gg, -1.55, 0.72, 1.7, -0.31, 0.6, new Color(0xe69646), Color.WHITE);
        });
        tile(g, 980, y + 22, w, h, px, "Ricochet: one hop on an arc|Execution: a white shatter", gg -> {
            dummy(gg, -1.2, 0.3);
            dummy(gg, 0.2, -0.2);
            gg.setColor(TowerArt.alpha(green, 0.8));
            stroke(gg, 0.022);
            Path2D.Double hop = new Path2D.Double();
            hop.moveTo(-1.2, 0.3);
            hop.quadTo(-0.6, -0.55, 0.2, -0.2);
            gg.draw(hop);
            gg.setColor(Color.WHITE);
            for (int i = 0; i < 8; i++) {
                double a = i * 45 + 10;
                gg.fill(TowerArt.at(TowerArt.poly(-0.05, -0.022, 0.06, 0, -0.05, 0.022), a, 0.3 + (i % 2) * 0.08).getBounds2D().isEmpty()
                        ? TowerArt.circ(0, 0, 0) : TowerArt.mv(TowerArt.at(TowerArt.poly(-0.05, -0.022, 0.06, 0, -0.05, 0.022), a, 0.3 + (i % 2) * 0.08), 1.5, 0.2));
            }
            gg.setColor(TowerArt.alpha(Color.WHITE, 0.5));
            stroke(gg, 0.02);
            gg.draw(TowerArt.circ(1.5, 0.2, 0.2));
        });
        y += h + 22 + 44;
        // Burst
        TowerArtSheets.text(g, "Burst, Stormcaller, Hexer", 20, y + 14, TowerArtSheets.TEXT, 13, true);
        tile(g, 20, y + 22, w, h, px, "The stone: a high arc, a shadow under it|100% core white, 50% edge fainter", gg -> {
            gg.translate(-1.6, 0.55);
            TowerArt.paintCell(gg, TowerArt.build("burst", 0, st().fire(1)), px);
            gg.translate(1.6, -0.55);
            lob(gg, -1.5, 0.45, 1.3, 0.35, 0.75, 4, stone(0xd8c8c0, false));
            blast(gg, 1.3, 0.35, 0.75 / 2, 0.75, 0);
        });
        tile(g, 340, y + 22, w, h, px, "Forked stones take the fork's colour|the Arc's charged stone is the brighter one", gg -> {
            lob(gg, -2.0, 0.3, -0.3, 0.25, 0.6, 3, stone(TowerArt.STORM, false));
            lob(gg, -0.2, 0.75, 1.9, 0.6, 0.75, 3, stone(0xdfe8ff, true));
            lob(gg, -1.2, 0.95, 0.4, 0.95, 0.4, 2, stone(TowerArt.HEXER, false));
        });
        tile(g, 660, y + 22, w, h, px, "Arc: hop by hop, a pause between|a crit hop is the thick white one", gg -> {
            double[][] at = {{-1.9, 0.5}, {-1.0, -0.3}, {0.0, 0.4}, {1.0, -0.2}, {1.9, 0.5}};
            for (double[] e : at) {
                dummy(gg, e[0], e[1]);
            }
            lightning(gg, bolt(at[0][0], at[0][1], at[1][0], at[1][1], 5, 0.12, 1), TowerArt.STORM, 0.02);
            lightning(gg, bolt(at[1][0], at[1][1], at[2][0], at[2][1], 5, 0.12, 2), TowerArt.STORM, 0.045);
            gg.setColor(TowerArt.alpha(TowerArt.STORM, 0.3));
            stroke(gg, 0.02);
            gg.draw(bolt(at[2][0], at[2][1], at[3][0], at[3][1], 5, 0.12, 3));
        });
        tile(g, 980, y + 22, w, h, px, "Wild Arc: one lightning bouncing back|and forth between two enemies", gg -> {
            dummy(gg, -1.2, 0.1);
            dummy(gg, 1.2, -0.1);
            lightning(gg, bolt(-1.2, 0.1, 1.2, -0.1, 7, 0.22, 5), TowerArt.STORM, 0.022);
            gg.setColor(TowerArt.alpha(TowerArt.STORM, 0.35));
            stroke(gg, 0.018);
            gg.draw(bolt(1.2, -0.1, -1.2, 0.1, 7, 0.22, 9));
        });
        y += h + 22 + 44;
        tile(g, 20, y, w, h, px, "Thunderclap: one discharge over the range|Static: a thin quiet spark to each tagger", gg -> {
            gg.translate(-0.9, 0.1);
            TowerArt.paintCell(gg, TowerArt.build("stormcaller", 3, st().fire(1)), px);
            gg.setColor(TowerArt.alpha(TowerArt.STORM, 0.5));
            stroke(gg, 0.03);
            gg.draw(TowerArt.circ(0, 0, 0.95));
            for (int i = 0; i < 7; i++) {
                lightning(gg, bolt(TowerArt.cos(i * 51) * 0.45, TowerArt.sin(i * 51) * 0.45, TowerArt.cos(i * 51 + 8) * 0.93,
                        TowerArt.sin(i * 51 + 8) * 0.93, 4, 0.07, i), TowerArt.STORM, 0.012);
            }
            dummy(gg, 2.5, 0.3);
            gg.setColor(TowerArt.alpha(0xc0c9ff, 0.7));
            stroke(gg, 0.012);
            gg.draw(bolt(2.5, 0.3, 0.2, 0.0, 9, 0.05, 4));
        });
        tile(g, 340, y, w, h, px, "Thunderstrike: a bolt from above|onto its enemy", gg -> {
            dummy(gg, 0, 0.6);
            lightning(gg, bolt(0.25, -1.3, 0, 0.6, 7, 0.16, 12), TowerArt.STORM, 0.04);
            gg.setColor(TowerArt.alpha(Color.WHITE, 0.7));
            stroke(gg, 0.02);
            gg.draw(TowerArt.ell(0, 0.62, 0.42, 0.2));
        });
        tile(g, 660, y, w, h, px, "Hexes fly as curses in their marker colours|Doom, Blight, Inversion", gg -> {
            gg.translate(-1.75, 0.2);
            TowerArt.paintCell(gg, TowerArt.build("hexer", 2, st().fire(1)), px);
            gg.translate(1.75, -0.2);
            curse(gg, -0.6, -0.6, -12, new Color(190, 90, 255));
            curse(gg, 0.3, 0.1, 4, new Color(140, 200, 60));
            curse(gg, 1.3, 0.75, 18, new Color(255, 70, 120));
        });
        tile(g, 980, y, w, h, px, "Soulbrand: the soul flies to each Hexer|Contagion: the hex jumps to the next enemy", gg -> {
            gg.translate(-1.7, -0.4);
            TowerArt.paintCell(gg, TowerArt.build("hexer", 1, st()), px);
            gg.translate(1.7, 0.4);
            for (int i = 0; i < 5; i++) {
                gg.setColor(TowerArt.alpha(0xf0e8ff, 0.85 - i * 0.16));
                gg.fill(TowerArt.circ(-0.7 + i * 0.13, -0.32 + i * 0.03, 0.05 - i * 0.007));
            }
            gg.setColor(TowerArt.alpha(0xf0e8ff, 0.5));
            stroke(gg, 0.016);
            gg.draw(TowerArt.circ(0.3, -0.2, 0.2));
            dummy(gg, 1.7, 0.6);
            gg.setColor(TowerArt.alpha(new Color(140, 200, 60), 0.5));
            gg.draw(TowerArt.circ(0.2, 0.75, 0.2));
            curse(gg, 1.0, 0.55, -8, new Color(140, 200, 60));
        });
        y += h + 44;
        // Mortar
        TowerArtSheets.text(g, "Mortar: every shell kind in flight, the same drawing as in the barrel and the pane", 20, y + 14, TowerArtSheets.TEXT, 13, true);
        String[] kinds = {"normal", "heavy", "nuke", "napalm", "tar", "cryo", "flare"};
        String[] names = {"Normal|dark iron ball", "Heavy|larger, bright band", "Nuke|hazard glow; a 75% ring", "Napalm|flame trail", "Tar|black, glossy",
            "Cryo|frost trail", "Flare|a spark, short trail"};
        for (int i = 0; i < kinds.length; i++) {
            String kind = kinds[i];
            tile(g, 20 + i * 184, y + 22, 174, h, px, names[i], gg -> {
                lob(gg, -1.1, 0.75, 0.7, 0.6, 0.9, 2, shellFlyer(kind));
                if (kind.equals("flare")) {
                    return;
                }
                blast(gg, 0.7, 0.6, kind.equals("nuke") ? 0.3 : 0.25, kind.equals("nuke") ? 0.62 : 0.5, kind.equals("nuke") ? 0.46 : 0);
            });
        }
        y += h + 22 + 44;
        // Aerie, Obelisk, Beacon
        TowerArtSheets.text(g, "Aerie, Obelisk, Beacon", 20, y + 14, TowerArtSheets.TEXT, 13, true);
        tile(g, 20, y + 22, w, h, px, "A missile leaves along its orbit, curves|smoothly, speeds up, never misses", gg -> {
            gg.translate(-1.5, 0.3);
            TowerArt.Build b = TowerArt.build("aerie", 2, st());
            TowerArt.paintCell(gg, b, px);
            TowerArt.nest(gg, 1, 150, b.footprint() + 0.06, "frost", false);
            gg.translate(1.5, -0.3);
            dummy(gg, 1.8, -0.5);
            flight(gg, "frost", new double[] {-1.5, -0.12, -0.6, -0.75, 0.8, -0.2, 1.8, -0.5}, 4);
        });
        tile(g, 340, y + 22, w, h, px, "Swarm: seven peel off in a fan|Ruby shards, Shade wisps", gg -> {
            gg.translate(-1.7, 0.2);
            TowerArt.Build b = TowerArt.build("ruby", 3, st());
            TowerArt.paintCell(gg, b, px);
            gg.translate(1.7, -0.2);
            for (int i = 0; i < 5; i++) {
                double a = -50 + i * 25;
                AffineTransform save = gg.getTransform();
                gg.translate(-1.7 + TowerArt.cos(a) * (0.7 + i * 0.22), 0.2 + TowerArt.sin(a) * (0.7 + i * 0.22));
                gg.rotate(Math.toRadians(a));
                TowerArt.missile(gg, "ruby", false);
                gg.setTransform(save);
            }
            for (int i = 0; i < 3; i++) {
                AffineTransform save = gg.getTransform();
                gg.translate(1.0 + i * 0.4, -0.6 + i * 0.5);
                gg.rotate(Math.toRadians(20 + i * 15));
                TowerArt.missile(gg, "shade", false);
                gg.setTransform(save);
            }
        });
        tile(g, 660, y + 22, w, h, px, "The Sundial: a veil that turns once from|the stone, 1 s of every 3, out to 4 cells", gg -> {
            gg.translate(-1.9, 0.7);
            gg.scale(0.55, 0.55);
            sundial(gg, -48, 4.0, false);
            TowerArt.paintCell(gg, TowerArt.build("obelisk", 2, st()), px);
        });
        tile(g, 980, y + 22, w, h, px, "Reveal: a short ray that fades|Spotlight: a faint steady beam; Glare: a glint", gg -> {
            gg.translate(-1.7, 0.35);
            dummy(gg, 1.5, -0.8);
            dummy(gg, 3.2, 0.1);
            gg.setColor(TowerArt.alpha(0xffe9a8, 0.16));
            gg.fill(TowerArt.poly(0, 0, 3.2, -0.12, 3.2, 0.32));
            gg.setColor(TowerArt.alpha(0xfff6d8, 0.9));
            stroke(gg, 0.028);
            gg.draw(TowerArt.line(0.2, -0.1, 1.3, -0.7));
            gg.setColor(Color.WHITE);
            gg.fill(TowerArt.star(3.32, -0.05, 4, 0.13, 0.025, 0));
            TowerArt.paintCell(gg, TowerArt.build("beacon", 3, st()), px);
        });
        y += h + 22 + 44;
        tile(g, 20, y, w, h, px, "Flashover: tongues leap from a hot enemy|to the ones beside it", gg -> {
            dummy(gg, -1.0, 0.2);
            dummy(gg, 0.1, -0.35);
            dummy(gg, 0.3, 0.7);
            for (double[] to : new double[][] {{0.1, -0.35}, {0.3, 0.7}}) {
                td.ui.TowerArtGame.paintFlashover(gg, -1.0, 0.2, to[0], to[1], 3);
            }
        });
        tile(g, 340, y, w, h, px, "Knell: the stone rocks and one ring|runs out to the field's edge", gg -> {
            gg.setColor(TowerArt.alpha(TowerArt.OBELISK, 0.12));
            gg.fill(TowerArt.circ(0, 0, 1.05));
            gg.setColor(TowerArt.alpha(TowerArt.OBELISK, 0.45));
            stroke(gg, 0.02);
            gg.draw(TowerArt.circ(0, 0, 1.05));
            gg.setColor(TowerArt.alpha(0xc9a56c, 0.8));
            stroke(gg, 0.03);
            gg.draw(TowerArt.circ(0, 0, 0.7));
            TowerArt.paintCell(gg, TowerArt.build("obelisk", 1, st().look("burden").fire(1)), px);
        });
        y += h + 40;
        g.dispose();
        TowerArtSheets.save(img.getSubimage(0, 0, img.getWidth(), (int) y), "shots.png");
    }

    static void sniperScene(Graphics2D g) {
        AffineTransform save = g.getTransform();
        g.translate(-1.85, 0.6);
        TowerArt.paintCell(g, TowerArt.build("sniper", 1, st().aim(-18).fire(1)), 64);
        g.setTransform(save);
        dummy(g, 1.9, -0.6);
    }

    /** Ghosts of one missile along a smooth path through the given points. */
    static void flight(Graphics2D g, String kind, double[] xy, int ghosts) {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(xy[0], xy[1]);
        path.curveTo(xy[2], xy[3], xy[4], xy[5], xy[6], xy[7]);
        g.setColor(TowerArt.alpha(TowerArt.missileColour(kind), 0.35));
        g.setStroke(new BasicStroke(0.012f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f, new float[] {0.04f, 0.05f}, 0f));
        g.draw(path);
        for (int i = 1; i <= ghosts; i++) {
            double t = Math.pow((double) i / (ghosts + 1), 0.7);
            double u = 1 - t;
            double x = u * u * u * xy[0] + 3 * u * u * t * xy[2] + 3 * u * t * t * xy[4] + t * t * t * xy[6];
            double y = u * u * u * xy[1] + 3 * u * u * t * xy[3] + 3 * u * t * t * xy[5] + t * t * t * xy[7];
            double dx = 3 * u * u * (xy[2] - xy[0]) + 6 * u * t * (xy[4] - xy[2]) + 3 * t * t * (xy[6] - xy[4]);
            double dy = 3 * u * u * (xy[3] - xy[1]) + 6 * u * t * (xy[5] - xy[3]) + 3 * t * t * (xy[7] - xy[5]);
            AffineTransform save = g.getTransform();
            g.translate(x, y);
            g.rotate(Math.atan2(dy, dx));
            TowerArt.missile(g, kind, false);
            g.setTransform(save);
        }
    }

    /** The Sundial's shade from a stone at 0,0. The floors are near-black, so it is a cool veil, not a dark shadow. */
    static void sundial(Graphics2D g, double headingDeg, double reach, boolean umbra) {
        AffineTransform save = g.getTransform();
        g.rotate(Math.toRadians(headingDeg));
        Color veil = umbra ? new Color(0x6a58b0) : TowerArt.col(TowerArt.OBELISK);
        g.setPaint(new java.awt.LinearGradientPaint(0f, 0f, (float) reach, 0f, new float[] {0f, 1f},
                new Color[] {TowerArt.alpha(veil, umbra ? 0.55 : 0.4), TowerArt.alpha(veil, 0.04)}));
        g.fill(TowerArt.poly(0, -0.07, reach, -0.26, reach, 0.26, 0, 0.07));
        g.setColor(TowerArt.alpha(TowerArt.mix(veil, Color.WHITE, 0.4), 0.55));
        g.setStroke(new BasicStroke(0.018f));
        g.draw(TowerArt.line(0.1, -0.075, reach, -0.26));
        g.setTransform(save);
    }
}
