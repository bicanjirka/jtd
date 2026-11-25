import td.ui.TowerArtGame;
import td.ui.render.LoadedShell;
import td.ui.render.TowerCue;
import td.ui.render.TowerLook;
import td.ui.render.TowerPose;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * The art sheets' pen (docs/art). Every tower is drawn by the game itself, through
 * td.ui.TowerArtGame: a sheet names a tower, a tier and a State, and the game's own tables build and paint it. What is
 * drawn here with plain shapes is only what the game does not paint yet: shots in flight, the nest's missiles, the
 * stream. Nothing here is game code; TowerArtSheets renders the sheets.
 */
final class TowerArt {

    /** How far up the screen a height of one cell lifts a point: the board is seen from the south. */
    static final double LIFT = -0.42;

    static final int SNIPER = rgb(TowerLook.SNIPER);
    static final int BURST = rgb(TowerLook.SPLASH);
    static final int STORM = rgb(TowerLook.STORMCALLER);
    static final int HEXER = rgb(TowerLook.HEXER);
    static final int MORTAR = rgb(TowerLook.MORTAR);
    static final int SCORCHER = rgb(TowerLook.CINDER);
    static final int AERIE = rgb(TowerLook.SEEKER);
    static final int OBELISK = rgb(TowerLook.PULSE);
    static final int BEACON = rgb(TowerLook.AURA);
    static final int RUBY = rgb(TowerLook.RUBY);
    static final int SHADE = rgb(TowerLook.SHADE);
    static final int SOUL = 0x7a54d8;
    static final int UPGRADE = 0x6ee08a;
    static final Color PAPER_GAP = new Color(0x0d0e10);

    /** Tier colours for the Upgrades panel and the unlock ring: Tier 0, I, II, III, a row per look. */
    static final TowerLook[] TIER_LOOKS = {TowerLook.SNIPER, TowerLook.SPLASH, TowerLook.STORMCALLER, TowerLook.HEXER,
        TowerLook.MORTAR, TowerLook.CINDER, TowerLook.SEEKER, TowerLook.PULSE, TowerLook.AURA};
    static final int[][] TIERS = tiers();

    private TowerArt() {
    }

    private static int rgb(TowerLook look) {
        return TowerArtGame.color(look).getRGB() & 0xffffff;
    }

    private static int[][] tiers() {
        int[][] tiers = new int[TIER_LOOKS.length][4];
        for (int i = 0; i < TIER_LOOKS.length; i++) {
            for (int t = 0; t < 4; t++) {
                tiers[i][t] = TowerArtGame.tierColor(TIER_LOOKS[i], t).getRGB() & 0xffffff;
            }
        }
        return tiers;
    }

    // ---- colour ----------------------------------------------------------------------------------------------

    static Color mix(Color a, Color b, double t) {
        return new Color((int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    static Color col(int rgb) {
        return new Color(rgb);
    }

    static Color alpha(Color c, double a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) Math.round(Math.clamp(a, 0, 1) * 255));
    }

    static Color alpha(int rgb, double a) {
        return alpha(new Color(rgb), a);
    }

    // ---- shapes, in cell units, centre of the cell at 0,0 ----------------------------------------------------

    static Shape circ(double x, double y, double r) {
        return new Ellipse2D.Double(x - r, y - r, r * 2, r * 2);
    }

    static Shape ell(double x, double y, double rx, double ry) {
        return new Ellipse2D.Double(x - rx, y - ry, rx * 2, ry * 2);
    }

    static Shape box(double x, double y, double w, double h) {
        return new Rectangle2D.Double(x - w / 2, y - h / 2, w, h);
    }

    /** A regular polygon, its first corner at {@code deg} (0 is east, -90 north). */
    static Shape ngon(double x, double y, int n, double r, double deg) {
        Path2D.Double p = new Path2D.Double();
        for (int i = 0; i < n; i++) {
            double a = Math.toRadians(deg + 360.0 * i / n);
            if (i == 0) {
                p.moveTo(x + Math.cos(a) * r, y + Math.sin(a) * r);
            } else {
                p.lineTo(x + Math.cos(a) * r, y + Math.sin(a) * r);
            }
        }
        p.closePath();
        return p;
    }

    static Shape star(double x, double y, int n, double outer, double inner, double deg) {
        Path2D.Double p = new Path2D.Double();
        for (int i = 0; i < n * 2; i++) {
            double a = Math.toRadians(deg + 180.0 * i / n);
            double r = i % 2 == 0 ? outer : inner;
            if (i == 0) {
                p.moveTo(x + Math.cos(a) * r, y + Math.sin(a) * r);
            } else {
                p.lineTo(x + Math.cos(a) * r, y + Math.sin(a) * r);
            }
        }
        p.closePath();
        return p;
    }

    static Shape poly(double... xy) {
        Path2D.Double p = new Path2D.Double();
        p.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) {
            p.lineTo(xy[i], xy[i + 1]);
        }
        p.closePath();
        return p;
    }

    static Shape open(double... xy) {
        Path2D.Double p = new Path2D.Double();
        p.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) {
            p.lineTo(xy[i], xy[i + 1]);
        }
        return p;
    }

    static Shape ring(double x, double y, double outer, double inner) {
        Area a = new Area(circ(x, y, outer));
        a.subtract(new Area(circ(x, y, inner)));
        return a;
    }

    static Shape minus(Shape a, Shape b) {
        Area area = new Area(a);
        area.subtract(new Area(b));
        return area;
    }

    static Shape rot(Shape s, double deg) {
        return AffineTransform.getRotateInstance(Math.toRadians(deg)).createTransformedShape(s);
    }

    static Shape mv(Shape s, double dx, double dy) {
        return AffineTransform.getTranslateInstance(dx, dy).createTransformedShape(s);
    }

    static Shape at(Shape s, double deg, double r) {
        double a = Math.toRadians(deg);
        return mv(rot(s, deg), Math.cos(a) * r, Math.sin(a) * r);
    }

    static double cos(double deg) {
        return Math.cos(Math.toRadians(deg));
    }

    static double sin(double deg) {
        return Math.sin(Math.toRadians(deg));
    }

    // ---- towers: the game's own -----------------------------------------------------------------------------

    /** A sheet's name for a look, with the Obelisk's rune picked by {@code State.look}. */
    static TowerLook look(String id, State s) {
        return switch (id) {
            case "sniper" -> TowerLook.SNIPER;
            case "burst" -> TowerLook.SPLASH;
            case "stormcaller" -> TowerLook.STORMCALLER;
            case "hexer" -> TowerLook.HEXER;
            case "mortar" -> TowerLook.MORTAR;
            case "scorcher" -> TowerLook.CINDER;
            case "soulfire" -> TowerLook.SOULFIRE;
            case "aerie" -> TowerLook.SEEKER;
            case "ruby" -> TowerLook.RUBY;
            case "shade" -> TowerLook.SHADE;
            case "white" -> TowerLook.WHITE;
            case "beacon" -> TowerLook.AURA;
            case "obelisk" -> switch (s.look) {
                case "burden" -> TowerLook.BURDEN;
                case "gravity" -> TowerLook.GRAVITY;
                case "umbra" -> TowerLook.UMBRA;
                default -> TowerLook.PULSE;
            };
            default -> throw new IllegalArgumentException(id);
        };
    }

    /** A tower the game builds: which look, tier and pose, and where its head aims. */
    record Build(TowerLook look, int tier, TowerPose pose, double aim, double top, double nest, double footprint) {

        /** How many parts the game builds it from. */
        int count() {
            return TowerArtGame.partCount(this.look, this.tier, this.pose);
        }
    }

    static Build build(String tower, int tier, State s) {
        TowerLook look = look(tower, s);
        return new Build(look, tier, s.pose(), s.aim, TowerArtGame.top(look, tier), TowerArtGame.nest(look, tier),
                TowerArtGame.footprint(tier));
    }

    /** Paints a tower centred on {@code cx, cy}, a cell {@code px} pixels wide. */
    static void paint(Graphics2D g, Build b, double cx, double cy, double px) {
        AffineTransform save = g.getTransform();
        g.translate(cx, cy);
        g.scale(px, px);
        paintCell(g, b, px);
        g.setTransform(save);
    }

    /** Paints a tower at 0,0 of a pen already in cell units. */
    static void paintCell(Graphics2D g, Build b, double px) {
        TowerArtGame.paint(g, b.look(), b.tier(), b.pose(), b.aim());
    }

    /** What the moving part shows. */
    static final class State {
        double aim = -35;
        /** 1 on the tick it fired, easing to 0. */
        double fire;
        /** 0 just fired, 1 ready. */
        double reload = 1;
        int stacks;
        /** The step of a running motion: Frenzy's shake, the Mortar's moment. */
        int beat;
        String look = "";
        String shell = "normal";
        final Set<String> cues = new HashSet<>();
        final List<Integer> panes = new ArrayList<>();

        static State idle() {
            return new State();
        }

        State aim(double deg) {
            this.aim = deg;
            return this;
        }

        State fire(double f) {
            this.fire = f;
            this.reload = 0;
            return this;
        }

        State reload(double r) {
            this.reload = r;
            return this;
        }

        State cue(String... names) {
            this.cues.addAll(List.of(names));
            return this;
        }

        State look(String name) {
            this.look = name;
            return this;
        }

        State shell(String kind) {
            this.shell = kind;
            return this;
        }

        State beat(int step) {
            this.beat = step;
            return this;
        }

        State stacks(int n) {
            this.stacks = n;
            return this;
        }

        State panes(Integer... colours) {
            this.panes.addAll(List.of(colours));
            return this;
        }

        boolean has(String cue) {
            return this.cues.contains(cue);
        }

        /** This state as the game's pose. A cue the game has no part for (the nest's count) is the sheet's own. */
        TowerPose pose() {
            TowerPose pose = TowerPose.idle();
            if (this.fire > 0.5) {
                pose = pose.fired();
            } else if (this.reload < 1) {
                pose = pose.refilling((float) this.reload);
            }
            pose = pose.withStacks(this.stacks).withBeat(this.beat).withLoad(java.util.List.of(LoadedShell.valueOf(this.shell.toUpperCase())));
            for (String cue : this.cues) {
                switch (cue) {
                    case "long" -> pose = pose.with(TowerCue.LONG_BARREL);
                    case "silver" -> pose = pose.with(TowerCue.SILVER_ROUND);
                    case "hollow" -> pose = pose.with(TowerCue.HOLLOW_POINT);
                    case "fourth" -> pose = pose.with(TowerCue.FOURTH_SHOT);
                    case "frenzy" -> pose = pose.with(TowerCue.FRENZY);
                    case "headhunter" -> pose = pose.with(TowerCue.HEADHUNTER);
                    case "full" -> pose = pose.with(TowerCue.CHARGED);
                    case "volley" -> pose = pose.with(TowerCue.VOLLEY);
                    case "stream" -> pose = pose.with(TowerCue.STREAMING);
                    case "dragon" -> pose = pose.with(TowerCue.DRAGONS_BREATH);
                    case "release" -> pose = pose.with(TowerCue.RELEASE_READY);
                    case "daylight" -> pose = pose.with(TowerCue.DAYLIGHT);
                    default -> { }
                }
            }
            if (!this.panes.isEmpty()) {
                List<TowerLook> lit = new ArrayList<>();
                for (int rgb : this.panes) {
                    for (TowerLook look : TowerLook.values()) {
                        if ((TowerArtGame.color(look).getRGB() & 0xffffff) == rgb) {
                            lit.add(look);
                            break;
                        }
                    }
                }
                pose = pose.withPanes(lit);
            }
            return pose;
        }
    }

    /** The nest, as the game draws it: {@code half} 1 paints it, -1 nothing, since no missile is drawn behind a tower. */
    static void nest(Graphics2D g, int count, double phaseDeg, double ringRadius, String kind, boolean bright, int half) {
        if (half >= 0) {
            td.ui.TowerArtGame.paintNest(g, count, phaseDeg, ringRadius, kind, bright);
        }
    }

    static void nest(Graphics2D g, int count, double phaseDeg, double ringRadius, String kind, boolean bright) {
        nest(g, count, phaseDeg, ringRadius, kind, bright, 0);
    }

    static Color missileColour(String kind) {
        return switch (kind) {
            case "ruby" -> col(RUBY);
            case "shade" -> col(SHADE);
            default -> col(AERIE);
        };
    }

    /** One missile heading +x, as the game draws it. */
    static void missile(Graphics2D g, String kind, boolean bright) {
        td.ui.TowerArtGame.paintMissile(g, kind, 0.12, bright);
    }

    // ---- shared marks ----------------------------------------------------------------------------------------

    /** The one "an upgrade is ready" mark, as the game draws it, for the cell's top-left corner. */
    static void upgradeArrow(Graphics2D g) {
        td.ui.TowerArtGame.paintUpgradeArrow(g);
    }

    static Line2D line(double x0, double y0, double x1, double y1) {
        return new Line2D.Double(x0, y0, x1, y1);
    }
}
