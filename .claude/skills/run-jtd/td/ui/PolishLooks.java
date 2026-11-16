package td.ui;

import td.ui.TowerFoundations.Kind;
import td.ui.TowerFoundations.Part;
import td.ui.render.LoadedShell;
import td.ui.render.TowerCue;
import td.ui.render.TowerLook;
import td.ui.render.TowerPose;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleFunction;

import static td.ui.TowerFoundations.at;
import static td.ui.TowerFoundations.box;
import static td.ui.TowerFoundations.circ;
import static td.ui.TowerFoundations.cos;
import static td.ui.TowerFoundations.minus;
import static td.ui.TowerFoundations.mix;
import static td.ui.TowerFoundations.mv;
import static td.ui.TowerFoundations.ngon;
import static td.ui.TowerFoundations.poly;
import static td.ui.TowerFoundations.rot;
import static td.ui.TowerFoundations.sin;

/**
 * The tower visual polish, drawn in the game's own part vocabulary (docs/features/FEATURE-tower-visual-polish.md).
 * Every builder here is written to be moved into {@code TowerFoundations} as it stands: the same measures (cell units,
 * the cell's centre at 0,0, north at -90 degrees), the same {@code Build} calls, the same pose reads. What this file
 * copies from {@code TowerFoundations} because it is private there ({@code Build}, {@code plinth}, {@code stoneRing},
 * the small shape and colour helpers) is marked COPY and is not moved.
 */
final class PolishLooks {

    /** As {@code TowerSprites.LIFT}: screen rise of a part's top per cell of height. */
    static final double LIFT = 0.42;

    // COPY of TowerFoundations' private constants.
    private static final double PLINTH_STEP = 0.045;
    private static final double STEP_HEIGHT = 0.07;
    private static final Color PLINTH_STONE = new Color(0x50535e);
    private static final Color PAVING_DARK = new Color(0x101014);
    private static final Color SHADE_STONE = new Color(0x5a6470);
    private static final Color SHADE_LIGHT = new Color(0xa6e8c8);
    private static final Color SOUL_FLAME = new Color(0xb9a0ff);
    private static final double[] SPARK_BEARINGS = {36, 124, 215, 294};

    // ---- NEW constants (move) ----------------------------------------------------------------------------------------

    /** The loaded shell, larger than it was (0.034), so a loaded mortar reads at the board's own size. */
    static final double BORE_SHELL = 0.05;
    /** The White tower's own colour (a TowerLooks row): pale marble. Its light is {@link #WHITE_LIGHT}. */
    static final Color WHITE_TOWER = new Color(0xe6eaf2);
    static final Color WHITE_LIGHT = new Color(0xfff4d6);
    static final Color WHITE_ROOF = new Color(0x8fa9d6);
    static final Color WHITE_GOLD = new Color(0xe8cf8a);
    static final Color RUBY_EMBER = new Color(0xff7a5a);
    /** The plane of the menhir's rune face, south of the stone's centre, in cells. */
    static final double MENHIR_FACE = 0.085;
    /** How far the menhir is thrown sideways at each step of a knell's recoil (1, 3/4, 1/2, 1/4, 0), in cells. */
    private static final double[] KNELL_SHAKE = {0, -0.005, 0.009, -0.013, 0.016};
    /** How tall a rune is drawn on the face against its width: the face is seen from above, a rune must still read. */
    static final double RUNE_SQUASH = 0.75;

    private PolishLooks() {
    }

    /** What a lab look is; the names are the {@code TowerLook} constants they become or already are. */
    enum Look {
        SPLASH, STORMCALLER, HEXER, CINDER, SOULFIRE, SEEKER, RUBY, SHADE,
        /** NEW TowerLook: the third Aerie identity, a white tower. */
        WHITE,
        PULSE, BURDEN, GRAVITY, UMBRA, MORTAR, SNIPER
    }

    /** How a live light lives. BREATH, FLICKER and GLINT are the game's; the rest are NEW {@code Life} constants. */
    enum Life {
        BREATH, FLICKER, GLINT,
        /** Tesla arcs that jump from a ball now and then: {@code PolishLife.sparks}. */
        SPARKS,
        /** Motes that turn inside a glass ball: {@code PolishLife.swirl}. */
        SWIRL,
        /** A rune's glow in a wave that climbs the stone: {@code PolishLife.rune}. */
        RUNE
    }

    /** A {@code TowerFoundations.Light} with the new lives. */
    record Live(Life life, double x, double y, double z, double radius, Color color, boolean turning) {
    }

    /** A {@code TowerFoundations.Foundation}; {@code nest} is the height of the ring missiles and birds circle on. */
    record Built(List<Part> parts, List<Part> head, List<Part> front, List<Live> lives, double top, double footprint,
                 double nest, double muzzle, double muzzleHeight) {

        static Built of(Build b) {
            List<Part> front = b.head.isEmpty() ? List.of() : List.copyOf(b.front);
            if (b.head.isEmpty()) {
                b.finish();
            }
            double muzzle = 0;
            double height = b.top;
            for (Part part : b.head) {
                double far = part.shape().getBounds2D().getMaxX();
                if (part.kind() == Kind.SOLID && far > muzzle) {
                    muzzle = far;
                    height = part.z0() + part.height() / 2;
                }
            }
            return new Built(List.copyOf(b.parts), List.copyOf(b.head), front, List.copyOf(b.lives), b.top, b.footprint,
                    b.nest, muzzle, height);
        }

        /** The game's own foundation, for a look this work leaves alone. */
        static Built of(TowerFoundations.Foundation f) {
            List<Live> lives = new ArrayList<>();
            for (TowerFoundations.Light l : f.lights()) {
                lives.add(new Live(Life.valueOf(l.life().name().equals("SWEEP") ? "BREATH" : l.life().name()), l.x(),
                        l.y(), l.z(), l.radius(), l.color(), l.turning()));
            }
            return new Built(f.parts(), f.head(), f.front(), lives, f.top(), f.footprint(), f.nest(), f.muzzle(),
                    f.muzzleHeight());
        }

        /** The game's body under a new head. */
        Built withHead(Build head) {
            Build b = new Build();
            b.head.addAll(head.parts);
            b.top = this.top;
            b.footprint = this.footprint;
            for (Live l : head.lives) {
                b.lives.add(new Live(l.life(), l.x(), l.y(), l.z(), l.radius(), l.color(), true));
            }
            Built h = Built.of(b);
            List<Live> lives = new ArrayList<>(this.lives.stream().filter(l -> !l.turning()).toList());
            lives.addAll(h.lives);
            return new Built(this.parts, h.head, this.front, lives, this.top, this.footprint, this.nest, h.muzzle,
                    h.muzzleHeight);
        }
    }

    /** The new look of {@code look} at {@code tier} in {@code pose}. */
    static Built of(Look look, int tier, TowerPose pose) {
        return switch (look) {
            case SPLASH -> tier == 0 ? Built.of(TowerFoundations.of(TowerLook.SPLASH, 0, pose))
                    : Built.of(thrower(tier, pose));
            case STORMCALLER -> Built.of(stormcaller(Math.max(1, tier), pose));
            case HEXER -> Built.of(hexer(Math.max(1, tier), pose));
            case CINDER -> {
                Built body = Built.of(TowerFoundations.of(TowerLook.CINDER, tier, pose));
                yield pose.has(TowerCue.DRAGONS_BREATH) ? body.withHead(dragonHead(body.top(), pose)) : body;
            }
            case SOULFIRE -> Built.of(TowerFoundations.of(TowerLook.SOULFIRE, tier, pose))
                    .withHead(soulHead(TowerFoundations.of(TowerLook.SOULFIRE, tier, pose).top(), pose));
            case SEEKER -> Built.of(aerie(tier, pose));
            case RUBY -> Built.of(ruby(tier, pose));
            case SHADE -> Built.of(shade(tier, pose));
            case WHITE -> Built.of(white(tier, pose));
            case PULSE -> Built.of(obelisk(tier, pose, TowerLook.PULSE));
            case BURDEN -> Built.of(obelisk(tier, pose, TowerLook.BURDEN));
            case GRAVITY -> Built.of(obelisk(tier, pose, TowerLook.GRAVITY));
            case UMBRA -> Built.of(obelisk(tier, pose, TowerLook.UMBRA));
            case MORTAR -> {
                Built body = Built.of(TowerFoundations.of(TowerLook.MORTAR, tier, pose));
                yield body.withHead(mortarHead(body.top() + 0.02, pose));
            }
            case SNIPER -> Built.of(TowerFoundations.of(TowerLook.SNIPER, tier, pose));
        };
    }

    // ==== COPY: helpers private in TowerFoundations ===================================================================

    private static Color alpha(Color c, double share) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) Math.round(Math.clamp(share, 0, 1) * 255));
    }

    private static Tones tones(int rgb) {
        return Tones.of(new Color(rgb));
    }

    private static Tones stone(Color tower) {
        return Tones.of(mix(PLINTH_STONE, tower, 0.24));
    }

    private static Color colour(TowerLook look) {
        return TowerLooks.color(look);
    }

    private static Shape ell(double x, double y, double rx, double ry) {
        return new Ellipse2D.Double(x - rx, y - ry, rx * 2, ry * 2);
    }

    private static Shape star(double x, double y, int n, double outer, double inner, double deg) {
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

    private static Shape open(double... xy) {
        Path2D.Double p = new Path2D.Double();
        p.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) {
            p.lineTo(xy[i], xy[i + 1]);
        }
        return p;
    }

    private static Shape ring(double x, double y, double outer, double inner) {
        return minus(circ(x, y, outer), circ(x, y, inner));
    }

    /** COPY of TowerFoundations.Build, with {@code light} taking the lab's lives. */
    static final class Build {

        private final List<Part> parts = new ArrayList<>();
        private final List<Part> front = new ArrayList<>();
        private final List<Part> head = new ArrayList<>();
        private final List<Live> lives = new ArrayList<>();
        private double top;
        private double nest;
        private double bodyRadius;
        private double footprint;

        Build solid(Shape s, double z0, double h, Tones t) {
            this.parts.add(Part.solid(s, z0, h, t));
            return this;
        }

        Build flat(Shape s, double z, Color c) {
            this.parts.add(Part.flat(Kind.FLAT, s, z, c));
            return this;
        }

        Build glow(double x, double y, double r, double z, Color c) {
            this.parts.add(Part.flat(Kind.GLOW, circ(x, y, r), z, c));
            return this;
        }

        Build line(Shape s, double z, Color c, double w) {
            this.parts.add(Part.line(s, z, c, w));
            return this;
        }

        Build light(Life life, double x, double y, double z, double r, Color c) {
            this.lives.add(new Live(life, x, y, z, r, c, false));
            return this;
        }

        Build aim(Build turning) {
            this.head.addAll(turning.parts);
            for (Live l : turning.lives) {
                this.lives.add(new Live(l.life(), l.x(), l.y(), l.z(), l.radius(), l.color(), true));
            }
            return this;
        }

        int mark() {
            return this.parts.size();
        }

        Build around(int from) {
            List<Part> added = new ArrayList<>(this.parts.subList(from, this.parts.size()));
            this.parts.subList(from, this.parts.size()).clear();
            for (Part p : added) {
                Rectangle2D r = p.shape().getBounds2D();
                if (r.getMaxY() <= 0.001) {
                    this.parts.add(p);
                } else if (r.getMinY() >= -0.001) {
                    this.front.add(p);
                } else if (p.kind() == Kind.SOLID || p.kind() == Kind.FLAT) {
                    Area north = new Area(p.shape());
                    north.intersect(new Area(new Rectangle2D.Double(-2, -2, 4, 2)));
                    Area south = new Area(p.shape());
                    south.intersect(new Area(new Rectangle2D.Double(-2, 0, 4, 2)));
                    this.parts.add(p.withShape(north));
                    this.front.add(p.withShape(south));
                } else if (r.getCenterY() <= 0) {
                    this.parts.add(p);
                } else {
                    this.front.add(p);
                }
            }
            return this;
        }

        Build finish() {
            this.parts.addAll(this.front);
            this.front.clear();
            return this;
        }
    }

    /** COPY of TowerFoundations.plinth. */
    private static Build plinth(int tier, Color tower, DoubleFunction<Shape> shape) {
        Build b = new Build();
        Tones stone = stone(tower);
        double r = TowerFoundations.footprint(tier);
        for (int i = 0; i < tier; i++) {
            b.solid(shape.apply(r - i * PLINTH_STEP), i * STEP_HEIGHT, STEP_HEIGHT, stone);
        }
        if (tier > 0) {
            b.flat(shape.apply(r - tier * PLINTH_STEP + PLINTH_STEP * 0.45), tier * STEP_HEIGHT,
                    mix(tower, PAVING_DARK, 0.66));
        }
        b.footprint = r;
        b.bodyRadius = r - tier * PLINTH_STEP;
        b.top = tier * STEP_HEIGHT;
        return b;
    }

    /** COPY of TowerFoundations.stoneRing. */
    private static void stoneRing(Build b, double r, double z, Color tower, boolean two) {
        int mark = b.mark();
        Tones a = stone(tower);
        Tones c = Tones.of(mix(a.body(), Color.WHITE, 0.16));
        for (int i = 0; i < 9; i++) {
            double size = 0.058 + 0.012 * ((i * 7) % 3);
            b.solid(at(ngon(0, 0, 5 + i % 2, size, i * 31), i * 40, r), z, 0.09 + 0.02 * (i % 2), i % 2 == 0 ? a : c);
        }
        if (two) {
            for (int i = 0; i < 5; i++) {
                b.solid(at(ngon(0, 0, 5, 0.046, i * 47), 20 + i * 72, r * 0.98), z + 0.10, 0.07, i % 2 == 0 ? c : a);
            }
        }
        b.around(mark);
    }

    // ==== NEW helpers (move) ==========================================================================================

    /** {@code s} scaled by {@code sx, sy} about {@code ax, ay}. */
    static Shape scaled(Shape s, double sx, double sy, double ax, double ay) {
        AffineTransform t = AffineTransform.getTranslateInstance(ax, ay);
        t.scale(sx, sy);
        t.translate(-ax, -ay);
        return t.createTransformedShape(s);
    }

    /** {@code s} mirrored across the head's own axis, so one side of a head is drawn once. */
    static Shape mirrored(Shape s) {
        return AffineTransform.getScaleInstance(1, -1).createTransformedShape(s);
    }

    /**
     * A ball as the board's light sees it: a dark disc, its lit body up and to the left, a glint. A ball is the same
     * circle from every side, so it is flat parts at its centre's height, never a solid.
     */
    private static void ball(Build b, double x, double y, double z, double r, Color body) {
        b.flat(circ(x, y, r), z, mix(body, Color.BLACK, 0.5));
        b.flat(circ(x - r * 0.10, y - r * 0.10, r * 0.86), z, body);
        b.flat(circ(x - r * 0.36, y - r * 0.40, r * 0.17), z, mix(body, Color.WHITE, 0.75));
    }

    // ==== Burst: the stone thrower past Tier 0 (NEW, move) ============================================================

    /**
     * The stone thrower grown into a siege engine for a Burst that took neither fork: a taller frame every tier, a
     * counterweight from Tier II, a walled yard with a banner at Tier III. The arm reads the pose as Tier 0's does.
     */
    private static Build thrower(int tier, TowerPose pose) {
        Color burst = colour(TowerLook.SPLASH);
        Tones wood = Tones.of(burst);
        Tones dark = Tones.of(wood.shade());
        Tones iron = tones(0x6a5a52);
        Build b = plinth(tier, burst, r -> circ(0, 0, r));
        double r = b.bodyRadius;
        double z = b.top;
        b.flat(circ(0, 0, r * 0.74), z, new Color(0x2b2422));
        stoneRing(b, r * 0.80, z, burst, tier >= 2);
        if (tier == 3) {
            int mark = b.mark();
            Tones pale = Tones.of(mix(stone(burst).body(), Color.WHITE, 0.3));
            for (int i = 0; i < 6; i++) {
                b.solid(at(box(0, 0, 0.06, 0.07), 30 + i * 60, r * 0.80), z + 0.17, 0.07, pale);
            }
            b.around(mark);
        }
        int pile = b.mark();
        int stones = 1 + tier * 2;
        for (int i = 0; i < stones; i++) {
            double a = 118 + i * 17;
            b.solid(at(ngon(0, 0, 6, 0.03, i * 23), a, r * (i % 2 == 0 ? 0.50 : 0.36)), z, 0.035, tones(0xd8c8c0));
        }
        b.around(pile);
        double frame = 0.14 + 0.09 * tier;
        double half = 0.075 + 0.006 * tier;
        if (tier >= 2) {
            b.solid(box(-0.05, -half, 0.05, 0.04), z, frame * 0.45, dark);
            b.solid(box(0.05, -half, 0.05, 0.04), z, frame * 0.45, dark);
        }
        b.solid(box(0, -half, 0.055, 0.05), z, frame, wood);
        double axle = z + frame - 0.02;
        b.solid(box(0, 0, 0.03, half * 2 + 0.03), axle, 0.025, iron);
        double arm = 0.17 + 0.03 * tier;
        double thick = 0.034 + 0.004 * tier;
        double cup = 0.046 + 0.004 * tier;
        if (pose.flash()) {
            if (tier >= 2) {
                b.solid(at(box(0, 0, 0.075, 0.075), 140, 0.06), axle - 0.10, 0.07, iron);
            }
            b.solid(rot(box(arm * 0.26, 0, arm * 0.52, thick), -40), axle + 0.06, 0.03, wood);
            b.solid(at(circ(0, 0, cup * 0.87), -40, arm * 0.56), axle + 0.12, 0.02, dark);
        } else {
            if (tier >= 2) {
                b.solid(at(box(0, 0, 0.075, 0.075), -40, 0.07), axle + 0.03, 0.07, iron);
            }
            b.solid(rot(box(arm / 2, 0, arm, thick), 140), axle, 0.03, wood);
            b.solid(at(circ(0, 0, cup), 140, arm), axle, 0.03, dark);
            if (pose.reload() > 0.5) {
                b.solid(at(ngon(0, 0, 6, cup * 0.70, 10), 140, arm), axle + 0.03, 0.03, tones(0xd8c8c0));
            }
        }
        if (tier >= 2) {
            b.solid(box(-0.05, half, 0.05, 0.04), z, frame * 0.45, dark);
            b.solid(box(0.05, half, 0.05, 0.04), z, frame * 0.45, dark);
        }
        b.solid(box(0, half, 0.055, 0.05), z, frame, wood);
        if (tier == 3) {
            int mark = b.mark();
            b.solid(circ(0.19, -0.15, 0.013), z, 0.46, tones(0xd8c8c0));
            b.flat(poly(0.19, -0.15, 0.30, -0.125, 0.19, -0.10), z + 0.46, burst);
            b.around(mark);
        }
        b.top = axle + 0.06;
        return b;
    }

    // ==== Stormcaller (REPLACES TowerFoundations.stormcaller) =========================================================

    /**
     * A tesla coil: a fat primary drum, a tall secondary wound in two coppers so its turns show on its side, and a
     * steel top-load carrying the ball far above the cell. Guard rings and insulator posts come with the tiers.
     */
    private static Build stormcaller(int tier, TowerPose pose) {
        Color storm = colour(TowerLook.STORMCALLER);
        Tones copper = tones(0xc98a5c);
        Tones copperDark = tones(0x9a6038);
        Tones iron = tones(0x596074);
        Tones steel = tones(0xdfe6f5);
        Tones blue = Tones.of(storm);
        Build b = plinth(tier, storm, r -> ngon(0, 0, 12, r, 15));
        double r = b.bodyRadius;
        double z = b.top;
        if (tier == 1) {
            stoneRing(b, r * 0.80, z, storm, false);
        } else {
            b.solid(ngon(0, 0, 8, r, 22.5), z, 0.05, iron);
            z += 0.05;
            for (int i = 0; i < 8; i++) {
                b.flat(at(circ(0, 0, 0.013), i * 45, r * 0.84), z, iron.highlight());
            }
        }
        if (tier == 3) {
            int mark = b.mark();
            for (int i = 0; i < 6; i++) {
                b.solid(at(circ(0, 0, 0.042), 30 + i * 60, r * 0.74), z, 0.20, steel);
                b.flat(at(circ(0, 0, 0.018), 30 + i * 60, r * 0.74), z + 0.20, blue.body());
            }
            b.around(mark);
        }
        double wide = 0.115 + 0.012 * tier;
        b.solid(circ(0, 0, wide), z, 0.07, copper);
        z += 0.07;
        b.solid(circ(0, 0, wide * 0.72), z, 0.05, iron);
        z += 0.05;
        int turns = 9 + 4 * tier;
        double foot = z;
        for (int i = 0; i < turns; i++) {
            double t = (double) i / (turns - 1);
            b.solid(circ(0, 0, 0.064 - 0.016 * t), z, 0.05, i % 2 == 0 ? copper : copperDark);
            z += 0.05;
        }
        if (tier >= 2) {
            int mark = b.mark();
            b.solid(ring(0, 0, wide + 0.025, wide - 0.012), foot + (z - foot) * 0.30, 0.03, blue);
            b.around(mark);
        }
        if (tier >= 3) {
            int mark = b.mark();
            b.solid(ring(0, 0, wide - 0.02, wide - 0.05), foot + (z - foot) * 0.66, 0.03, blue);
            b.around(mark);
        }
        double load = 0.086 + 0.008 * tier;
        b.solid(circ(0, 0, load), z, 0.045, steel);
        z += 0.045;
        boolean charged = pose.has(TowerCue.CHARGED);
        double ball = 0.05 + 0.004 * tier;
        if (charged) {
            b.glow(0, 0, 0.28, z + ball, alpha(storm, 0.75));
        } else {
            b.light(Life.BREATH, 0, 0, z + ball, 0.19, storm);
            b.light(Life.SPARKS, 0, 0, z + ball, 0.34, new Color(0xe6edff));
        }
        ball(b, 0, 0, z + ball, ball, charged ? new Color(0xe8eeff) : storm);
        if (pose.flash()) {
            b.glow(0, 0, 0.22, z + ball, alpha(new Color(0xdfe8ff), 0.8));
            for (double a : SPARK_BEARINGS) {
                b.line(open(cos(a) * 0.05, sin(a) * 0.05, cos(a + 22) * 0.11, sin(a + 22) * 0.11,
                        cos(a - 8) * 0.15, sin(a - 8) * 0.15, cos(a + 10) * 0.21, sin(a + 10) * 0.21), z + ball,
                        new Color(0xe6edff), 0.016);
            }
        }
        b.top = z + ball * 2;
        return b;
    }

    // ==== Hexer (REPLACES TowerFoundations.hexer) =====================================================================

    /**
     * An oracle's ball of violet glass held high on an iron stem: claws cradle it, standing stones and tusks come with
     * the tiers. The ball clouds over while idle, glows when its ability waits and flares as it casts.
     */
    private static Build hexer(int tier, TowerPose pose) {
        Color hex = colour(TowerLook.HEXER);
        Tones iron = tones(0x555062);
        Tones bone = tones(0xe6dcc4);
        Color glass = mix(hex, new Color(0x3a1c60), 0.45);
        Color lit = Tones.of(hex).highlight();
        Build b = plinth(tier, hex, r -> ngon(0, 0, 12, r, 15));
        double r = b.bodyRadius;
        double z = b.top;
        if (tier == 1) {
            stoneRing(b, r * 0.80, z, hex, false);
        }
        if (tier == 3) {
            b.line(circ(0, 0, r * 0.94), z, alpha(hex, 0.9), 0.014);
            for (int i = 0; i < 6; i++) {
                b.line(open(cos(i * 60) * r * 0.86, sin(i * 60) * r * 0.86, cos(i * 60) * r, sin(i * 60) * r), z,
                        alpha(hex, 0.9), 0.014);
            }
        }
        if (tier >= 2) {
            int mark = b.mark();
            for (int i = 0; i < 3; i++) {
                b.solid(rot(box(0.16, 0, 0.17, 0.034), 90 + i * 120), z, 0.05, iron);
                b.solid(at(ngon(0, 0, 4, 0.052, 20), 30 + i * 120, r * 0.74), z, 0.16, stone(hex));
                b.flat(at(box(0, 0, 0.012, 0.045), 30 + i * 120, r * 0.74), z + 0.16, lit);
            }
            b.around(mark);
        }
        b.solid(ngon(0, 0, 8, tier >= 2 ? 0.13 : 0.11, 22.5), z, 0.08, iron);
        z += 0.08;
        double stem = 0.42 + 0.20 * tier;
        int twists = (int) Math.round(stem / 0.055);
        for (int i = 0; i < twists; i++) {
            b.solid(ngon(0, 0, 4, 0.052, i * 30), z, stem / twists, i % 2 == 0 ? iron : Tones.of(iron.highlight()));
            z += stem / twists;
        }
        double ball = 0.12 + 0.014 * tier;
        b.solid(circ(0, 0, 0.07), z, 0.03, iron);
        z += 0.03;
        double centre = z + ball * 0.9;
        Tones claw = tier == 3 ? bone : iron;
        for (int i = 0; i < 3; i++) {
            if (sin(-90 + i * 120) < 0) {
                b.solid(at(box(0, 0, 0.05, 0.026), -90 + i * 120, ball * 0.86), z, ball * 0.95, claw);
            }
        }
        boolean charged = pose.has(TowerCue.CHARGED);
        if (charged || pose.flash()) {
            b.glow(0, 0, ball * (pose.flash() ? 2.6 : 2.0), centre, alpha(hex, 0.8));
        } else {
            b.light(Life.BREATH, 0, 0, centre, ball * 1.7, hex);
            b.light(Life.SWIRL, 0, 0, centre, ball * 0.86, lit);
        }
        ball(b, 0, 0, centre, ball, pose.flash() ? mix(lit, Color.WHITE, 0.6) : charged ? lit : glass);
        if (pose.flash()) {
            b.flat(circ(-ball * 1.5, -0.06, 0.02), centre, lit);
            b.flat(circ(ball * 1.3, -0.12, 0.016), centre, lit);
            b.flat(circ(0.05, ball * 1.6, 0.018), centre, lit);
        }
        for (int i = 0; i < 3; i++) {
            if (sin(-90 + i * 120) >= 0) {
                b.solid(at(box(0, 0, 0.05, 0.026), -90 + i * 120, ball * 0.86), z, ball * 0.95, claw);
            }
        }
        b.top = centre + ball;
        return b;
    }

    // ==== Scorcher: the dragon's head (NEW head for DRAGONS_BREATH, move) =============================================

    /**
     * A dragon's head for a Scorcher that breathes: neck, crest, a lower jaw that drops while it streams, skull, brow,
     * horns swept back. Built along +X from prisms at their own heights, so turning it in plan turns it in space.
     */
    static Build dragonHead(double z, TowerPose pose) {
        Tones iron = tones(0x565a66);
        Tones scale = tones(0xb4452c);
        Tones crest = tones(0x8a2f22);
        Tones belly = tones(0xe2a04a);
        Tones bone = tones(0xe6dcc4);
        boolean streaming = pose.has(TowerCue.STREAMING);
        double x = pose.has(TowerCue.LONG_BARREL) ? 0.06 : 0;
        Color maw = streaming ? new Color(0xfff2c0) : mix(new Color(0xff5a2a), new Color(0x3a1410), pose.reload());
        Color eye = streaming ? Color.WHITE : new Color(0xffe066);
        double gape = streaming ? 0.04 : 0.012;
        Build h = new Build();
        h.solid(circ(0, 0, 0.075), z, 0.03, iron);
        h.solid(ell(-0.015 + x / 2, 0, 0.07 + x / 2, 0.062), z + 0.03, 0.10, scale);
        h.solid(poly(-0.17, 0, -0.07, -0.026, -0.07, 0.026), z + 0.05, 0.05, crest);
        h.solid(mv(ell(0.02, 0, 0.062, 0.056), x, 0), z + 0.13, 0.04, scale);
        h.solid(poly(-0.13 + x, 0, -0.04 + x, -0.024, -0.04 + x, 0.024), z + 0.13, 0.06, crest);
        double jaw = z + 0.125;
        h.solid(mv(poly(0.0, -0.066, 0.22, -0.042, 0.295, 0, 0.22, 0.042, 0.0, 0.066), x, 0), jaw - gape, 0.035,
                belly);
        h.flat(mv(poly(0.07, -0.034, 0.275, 0, 0.07, 0.034), x, 0), jaw + 0.035 - gape, maw);
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < 3; i++) {
                h.flat(mv(poly(0.12 + i * 0.045, side * 0.044, 0.142 + i * 0.045, side * 0.020, 0.164 + i * 0.045,
                        side * 0.040), x, 0), jaw + 0.035 - gape, bone.highlight());
            }
        }
        double skull = jaw + 0.05;
        for (int side = -1; side <= 1; side += 2) {
            h.solid(mv(poly(-0.02, side * 0.088, -0.16, side * 0.158, -0.08, side * 0.080), x, 0), skull + 0.005,
                    0.025, bone);
        }
        h.solid(mv(poly(-0.125, 0, -0.095, -0.092, 0.04, -0.086, 0.135, -0.054, 0.30, -0.038, 0.35, 0, 0.30, 0.038,
                0.135, 0.054, 0.04, 0.086, -0.095, 0.092), x, 0), skull, 0.065, scale);
        h.flat(mv(ell(0.318, -0.013, 0.010, 0.006), x, 0), skull + 0.065, new Color(0x2a0e0a));
        h.flat(mv(ell(0.318, 0.013, 0.010, 0.006), x, 0), skull + 0.065, new Color(0x2a0e0a));
        h.solid(mv(poly(0.09, 0, 0.13, -0.012, 0.27, 0, 0.13, 0.012), x, 0), skull + 0.065, 0.012, crest);
        h.solid(mv(poly(-0.13, 0, -0.105, -0.072, 0.03, -0.066, 0.095, 0, 0.03, 0.066, -0.105, 0.072), x, 0),
                skull + 0.065, 0.04, scale);
        for (int side = -1; side <= 1; side += 2) {
            h.flat(mv(poly(0.035, side * 0.084, 0.115, side * 0.058, 0.055, side * 0.050), x, 0), skull + 0.065, eye);
            h.flat(mv(circ(0.07, side * 0.064, 0.009), x, 0), skull + 0.065, new Color(0x2a0e0a));
            h.solid(mv(poly(-0.05, side * 0.062, -0.27, side * 0.145, -0.095, side * 0.028), x, 0), skull + 0.105,
                    0.04, bone);
        }
        if (streaming) {
            h.glow(0.30 + x, 0, 0.13, jaw + 0.03, alpha(new Color(0xffd34d), 0.85));
        } else {
            h.flat(mv(poly(0.285, -0.014, 0.325, 0, 0.285, 0.014), x, 0), jaw + 0.035 - gape, new Color(0xffd34d));
            h.light(Life.FLICKER, 0.305 + x, 0, jaw + 0.03, 0.06, new Color(0xffd34d));
        }
        return h;
    }

    // ==== Scorcher: Soulfire's head (REPLACES the nozzle for SOULFIRE, move) ==========================================

    /**
     * A soul cage: a dark barrel banded in pale violet, two blades and a fang holding a pale soul flame at its tip, a
     * small skull for a counterweight. The flame whitens while it streams; the blades cool as the nozzle did.
     */
    static Build soulHead(double z, TowerPose pose) {
        Tones metal = new Tones(new Color(0x1f1b2a), new Color(0x3d3650), new Color(0x8a78c0));
        Tones iron = tones(0x3a3648);
        Tones bone = tones(0xd9d2e6);
        boolean streaming = pose.has(TowerCue.STREAMING);
        double len = pose.has(TowerCue.LONG_BARREL) ? 0.27 : 0.21;
        Color soul = new Color(0xd5e6ff);
        Color tip = streaming ? new Color(0xf2ecff) : mix(SOUL_FLAME, iron.body(), pose.reload());
        Build h = new Build();
        h.solid(circ(0, 0, 0.066), z, 0.03, iron);
        h.solid(box(len / 2, 0, len, 0.058), z + 0.03, 0.036, metal);
        h.flat(box(len * 0.45, 0, 0.022, 0.058), z + 0.066, SOUL_FLAME);
        h.flat(box(len * 0.75, 0, 0.022, 0.058), z + 0.066, SOUL_FLAME);
        h.solid(circ(-0.055, 0, 0.042), z + 0.03, 0.05, bone);
        h.flat(circ(-0.045, -0.016, 0.011), z + 0.08, new Color(0x1f1b2a));
        h.flat(circ(-0.045, 0.016, 0.011), z + 0.08, new Color(0x1f1b2a));
        for (int side = -1; side <= 1; side += 2) {
            h.solid(poly(len - 0.03, side * 0.03, len + 0.02, side * 0.082, len + 0.125, side * 0.06, len + 0.06,
                    side * 0.045), z + 0.03, 0.04, Tones.of(tip));
        }
        double orb = streaming ? 0.04 : 0.03;
        h.glow(len + 0.065, 0, streaming ? 0.13 : 0.085, z + 0.06, alpha(SOUL_FLAME, streaming ? 0.95 : 0.7));
        h.flat(circ(len + 0.065, 0, orb), z + 0.06, streaming ? Color.WHITE : soul);
        h.solid(poly(len - 0.02, -0.014, len + 0.06, 0, len - 0.02, 0.014), z + 0.075, 0.02, metal);
        if (!streaming) {
            h.light(Life.FLICKER, len + 0.065, 0, z + 0.06, 0.075, soul);
        }
        return h;
    }

    // ==== Mortar: a head whose reload shows (REPLACES TowerFoundations.mortarHead) ====================================

    /**
     * The wide barrel with a loading cradle behind it. The reload is four moments a quarter apart: the bore smokes
     * empty, the next shell lies on the cradle, the loader's arm carries it over the bore, it drops in and glints.
     */
    static Build mortarHead(double z, TowerPose pose) {
        Tones brown = Tones.of(mix(colour(TowerLook.MORTAR), Color.WHITE, 0.12));
        Tones iron = tones(0x6a7080);
        double len = (pose.has(TowerCue.LONG_BARREL) ? 0.25 : 0.21) - pose.recoil() * 0.05;
        boolean volley = pose.has(TowerCue.VOLLEY);
        double bore = len - 0.06;
        double reload = pose.reload();
        Build h = new Build();
        h.solid(box(0, 0, 0.055, 0.21), z, 0.05, iron);
        h.solid(box(-0.125, 0, 0.11, 0.13), z, 0.035, iron);
        if (volley) {
            h.solid(box(-0.105, 0, 0.06, 0.19), z, 0.04, iron);
        }
        if (reload >= 0.25 && reload < 0.5) {
            shell(h, pose.shell(), -0.135, 0, BORE_SHELL, z + 0.035);
        }
        if (volley && reload >= 0.25) {
            for (int i = -1; i <= 1; i += 2) {
                shell(h, pose.shell(), -0.105, i * 0.068, 0.026, z + 0.04);
            }
        }
        h.solid(box(len / 2 - 0.04, 0, len + 0.04, 0.15), z, 0.10, brown);
        h.solid(box(len - 0.015, 0, 0.04, 0.178), z, 0.11, Tones.of(brown.highlight()));
        h.flat(ell(bore, 0, 0.060, 0.064), z + 0.11, new Color(0x17130f));
        if (pose.flash()) {
            h.glow(len + 0.02, 0, 0.14, z + 0.11, alpha(new Color(0xffe6b0), 0.9));
        }
        if (reload < 0.25 && !pose.flash()) {
            h.glow(bore, 0, 0.075, z + 0.13, alpha(new Color(0xb9bcc8), 0.55));
        }
        if (reload >= 0.5 && reload < 0.75) {
            h.solid(box((bore - 0.135) / 2, 0.06, bore + 0.135, 0.02), z + 0.22, 0.016, iron);
            h.solid(box(-0.135, 0.06, 0.034, 0.034), z + 0.035, 0.185, iron);
            shell(h, pose.shell(), bore, 0, BORE_SHELL, z + 0.21);
        }
        if (reload >= 0.75) {
            shell(h, pose.shell(), bore, 0, BORE_SHELL, z + 0.11);
        }
        if (reload >= 1) {
            h.light(Life.GLINT, bore - 0.012, -0.012, z + 0.13, 0.075, Color.WHITE);
        }
        return h;
    }

    /** A shell of {@code kind}: the game's own drawing (TowerFoundations.shell), lifted to its place. */
    private static void shell(Build h, LoadedShell kind, double x, double y, double r, double z) {
        for (Part p : TowerFoundations.shell(kind, r)) {
            h.parts.add(new Part(p.kind(), mv(p.shape(), x, y), p.z0() + z, p.height(), p.tones(), p.color(),
                    p.width()));
        }
    }

    // ==== Aerie (REPLACES TowerFoundations.aerie for SEEKER) ==========================================================

    /**
     * The plain Aerie: a crystal spire that narrows as it climbs, the tallest thing on the board. Its perch ring sits
     * near its top, where the missiles and the birds circle.
     */
    private static Build aerie(int tier, TowerPose pose) {
        Color tint = colour(TowerLook.SEEKER);
        Tones crystal = Tones.of(tint);
        Tones pale = Tones.of(crystal.highlight());
        Color lit = crystal.highlight();
        Build b = plinth(tier, tint, r -> ngon(0, 0, 12, r, 15));
        double r = b.bodyRadius;
        double z = b.top;
        boolean release = pose.has(TowerCue.RELEASE_READY);
        b.solid(ngon(0, 0, 6, r * 0.78, 30), z, 0.08, Tones.of(mix(tint, new Color(0x10202c), 0.5)));
        z += 0.08;
        if (tier >= 2) {
            int shards = tier == 3 ? 6 : 4;
            int mark = b.mark();
            for (int i = 0; i < shards; i++) {
                double a = (tier == 3 ? 0 : 45) + i * 360.0 / shards;
                b.solid(at(poly(-0.05, 0, 0, -0.026, 0.06, 0, 0, 0.026), a, r * 0.52), z, 0.20, pale);
            }
            b.around(mark);
        }
        double height = 1.00 + 0.40 * tier;
        double wide = 0.105 + 0.010 * tier;
        double[] share = {0.40, 0.30, 0.20, 0.10};
        double[] taper = {1.0, 0.80, 0.58, 0.34};
        double foot = z;
        for (int i = 0; i < share.length; i++) {
            Shape plan = tier == 3 && i == 0 ? star(0, 0, 6, 0.15, 0.095, 0) : ngon(0, 0, 6, wide * taper[i], 0);
            b.solid(plan, z, height * share[i], i == 3 ? pale : crystal);
            z += height * share[i];
        }
        b.flat(ngon(0, 0, 6, wide * 0.2, 30), z, mix(lit, Color.WHITE, 0.55));
        b.light(Life.BREATH, 0, 0, z, 0.15, lit);
        b.top = z;
        b.nest = foot + height * 0.80;
        perch(b, tier, release, crystal, stone(tint), lit);
        return b;
    }

    /** The ring the nest circles on, round the spire at the nest's height; arms carry it at Tier III. */
    private static void perch(Build b, int tier, boolean release, Tones ring, Tones arm, Color lit) {
        if (tier == 0) {
            return;
        }
        double z = b.nest - 0.03;
        int mark = b.mark();
        if (release) {
            b.glow(0, 0, 0.30, z, alpha(lit, 0.7));
        }
        if (tier == 3) {
            for (int i = 0; i < 3; i++) {
                b.solid(rot(box(0.12, 0, 0.14, 0.026), -90 + i * 120), z, 0.02, arm);
            }
        }
        b.solid(ring(0, 0, 0.20, 0.172), z, 0.022, release ? Tones.of(mix(lit, Color.WHITE, 0.5)) : ring);
        b.around(mark);
    }

    // ==== Aerie: Ruby (REPLACES the RUBY row) =========================================================================

    /** One crystal of a cluster: a faceted prism that narrows once and leans {@code lean} out along {@code bearing}. */
    private static void crystalShard(Build b, double x, double y, double r, double z, double h, int sides, double turn,
                                     double bearing, double lean, Tones tones) {
        double dx = cos(bearing) * lean;
        double dy = sin(bearing) * lean;
        b.solid(ngon(x, y, sides, r, turn), z, h * 0.62, tones);
        b.solid(ngon(x + dx, y + dy, sides, r * 0.68, turn), z + h * 0.62, h * 0.26, tones);
        b.solid(ngon(x + dx * 1.6, y + dy * 1.6, sides, r * 0.34, turn), z + h * 0.88, h * 0.12,
                Tones.of(tones.highlight()));
    }

    /**
     * A geode broken open: a cluster of ruby crystals leaning out of dark rock round one great shard, an ember in the
     * crack under them. No ring and no spire: the cluster is the silhouette.
     */
    private static Build ruby(int tier, TowerPose pose) {
        Color tint = colour(TowerLook.RUBY);
        Tones crystal = Tones.of(tint);
        Tones deep = Tones.of(mix(tint, new Color(0x3a0818), 0.45));
        Tones rose = Tones.of(mix(tint, new Color(0xffc0c8), 0.40));
        Tones rock = tones(0x3a2c34);
        Build b = plinth(tier, tint, r -> ngon(0, 0, 12, r, 15));
        double r = b.bodyRadius;
        double z = b.top;
        boolean release = pose.has(TowerCue.RELEASE_READY);
        b.solid(ngon(0, 0, 7, r * 0.86, 12), z, 0.07, rock);
        b.solid(ngon(0.01, 0, 7, r * 0.66, 40), z + 0.07, 0.05, Tones.of(rock.highlight()));
        z += 0.12;
        b.flat(ngon(0, 0, 7, r * 0.50, 12), z, mix(RUBY_EMBER, new Color(0x40101a), 0.45));
        b.glow(0, 0, r * 0.75, z, alpha(RUBY_EMBER, 0.55));
        double height = 0.80 + 0.34 * tier;
        double[] bearing = {205, 335, 95, 30, 150, 270};
        double[] far = {0.125, 0.135, 0.12, 0.15, 0.15, 0.11};
        double[] tall = {0.56, 0.44, 0.50, 0.30, 0.36, 0.40};
        double[] thick = {0.058, 0.05, 0.054, 0.042, 0.044, 0.046};
        int shards = 3 + tier;
        int mark = b.mark();
        for (int i = 0; i < shards; i++) {
            Tones t = i % 3 == 0 ? rose : i % 3 == 1 ? deep : crystal;
            crystalShard(b, cos(bearing[i]) * far[i], sin(bearing[i]) * far[i], thick[i], z, height * tall[i],
                    4 + i % 2, bearing[i] + 20, bearing[i], 0.022, t);
        }
        b.around(mark);
        double wide = 0.088 + 0.010 * tier;
        b.solid(ngon(0, -0.01, 6, wide, 15), z, height * 0.58, crystal);
        b.solid(ngon(0.008, -0.014, 6, wide * 0.74, 15), z + height * 0.58, height * 0.27, crystal);
        b.solid(ngon(0.014, -0.018, 4, wide * 0.42, 15), z + height * 0.85, height * 0.15, rose);
        z += height;
        b.flat(ngon(0.014, -0.018, 4, wide * 0.18, 15), z, release ? Color.WHITE : rose.highlight());
        if (release) {
            b.glow(0.014, -0.018, 0.26, z, alpha(rose.highlight(), 0.8));
        }
        b.light(Life.BREATH, 0.014, -0.018, z, 0.16, RUBY_EMBER);
        b.light(Life.GLINT, -0.03, -0.03, z - height * 0.3, 0.07, Color.WHITE);
        b.top = z;
        b.nest = z - height * 0.20;
        return b;
    }

    // ==== Aerie: Shade (REPLACES the SHADE row) =======================================================================

    /**
     * A dark tower: four buttress fins at its foot, a shaft that steps in with spikes on every ledge, and two horns
     * at its top with nothing between them. Its only light is pale green: slits down its face, the tips of its spikes.
     */
    private static Build shade(int tier, TowerPose pose) {
        Tones dark = new Tones(new Color(0x1c1e27), new Color(0x3d4150), new Color(0x8d98ac));
        Tones darker = new Tones(new Color(0x15161d), new Color(0x2c2f3b), new Color(0x6f7a8e));
        Build b = plinth(tier, SHADE_STONE, r -> ngon(0, 0, 12, r, 15));
        double r = b.bodyRadius;
        double z = b.top;
        boolean release = pose.has(TowerCue.RELEASE_READY);
        b.solid(star(0, 0, 4, r * 0.98, r * 0.50, 45), z, 0.12, darker);
        z += 0.12;
        if (tier >= 1) {
            b.solid(star(0, 0, 4, r * 0.74, r * 0.44, 0), z, 0.12, dark);
            z += 0.12;
        }
        if (tier == 3) {
            int mark = b.mark();
            for (int i = 0; i < 4; i++) {
                b.solid(at(ngon(0, 0, 3, 0.04, 0), 45 + i * 90, r * 0.80), b.top + 0.12, 0.26, darker);
                b.flat(at(circ(0, 0, 0.012), 45 + i * 90, r * 0.80), b.top + 0.38, SHADE_LIGHT);
            }
            b.around(mark);
        }
        double height = 0.68 + 0.30 * tier;
        double[] side = {0.21, 0.165, 0.125};
        double[] share = {0.46, 0.32, 0.22};
        double foot = z;
        for (int i = 0; i < 3; i++) {
            b.solid(box(0, 0, side[i], side[i]), z, height * share[i], dark);
            z += height * share[i];
            if (i < 2 && tier >= 2) {
                for (int k = 0; k < 4; k++) {
                    double a = 45 + k * 90;
                    b.solid(mv(ngon(0, 0, 3, 0.026, a), cos(a) * side[i] * 0.62, sin(a) * side[i] * 0.62), z, 0.09,
                            darker);
                }
            }
            double face = side[i] / 2;
            double from = z - height * share[i];
            b.line(open(0, face - LIFT * (from + height * share[i] * 0.30), 0,
                    face - LIFT * (from + height * share[i] * 0.72)), 0, SHADE_LIGHT, 0.014);
        }
        double horn = 0.20 + 0.04 * tier;
        for (int s = -1; s <= 1; s += 2) {
            b.solid(poly(s * 0.062, -0.036, s * 0.062, 0.036, s * 0.014, 0), z, horn * 0.6, dark);
            b.solid(poly(s * 0.060, -0.020, s * 0.060, 0.020, s * 0.034, 0), z + horn * 0.6, horn * 0.4, dark);
            b.flat(circ(s * 0.05, 0, 0.008), z + horn, SHADE_LIGHT);
        }
        b.flat(box(0, 0, 0.03, 0.05), z, release ? Color.WHITE : SHADE_LIGHT);
        if (release) {
            b.glow(0, 0, 0.26, z, alpha(SHADE_LIGHT, 0.8));
        }
        b.light(Life.BREATH, 0, 0, z, 0.14, SHADE_LIGHT);
        b.top = z + horn;
        b.nest = foot + height * 0.86;
        return b;
    }

    // ==== Aerie: the White tower (NEW look WHITE, move) ===============================================================

    /**
     * A white tower: a round marble shaft over a ring of small turrets, a balcony, a silver-blue roof drawn to a gold
     * needle with a pennant. Pure and plain: no crystal, no spikes, one warm light under its roof.
     */
    private static Build white(int tier, TowerPose pose) {
        Tones marble = new Tones(new Color(0x9aa3b6), WHITE_TOWER, Color.WHITE);
        Tones roof = Tones.of(WHITE_ROOF);
        Tones gold = Tones.of(WHITE_GOLD);
        Color window = new Color(0x4a5a80);
        Build b = plinth(tier, WHITE_TOWER, r -> ngon(0, 0, 12, r, 15));
        double r = b.bodyRadius;
        double z = b.top;
        boolean release = pose.has(TowerCue.RELEASE_READY);
        b.solid(circ(0, 0, r * 0.80), z, 0.08, marble);
        z += 0.08;
        if (tier >= 2) {
            int turrets = tier == 3 ? 6 : 3;
            int mark = b.mark();
            for (int i = 0; i < turrets; i++) {
                double a = (tier == 3 ? 0 : 30) + i * 360.0 / turrets;
                double x = cos(a) * r * 0.60;
                double y = sin(a) * r * 0.60;
                b.solid(circ(x, y, 0.042), z, 0.22, marble);
                b.solid(circ(x, y, 0.046), z + 0.22, 0.04, roof);
                b.solid(circ(x, y, 0.024), z + 0.26, 0.04, roof);
            }
            b.around(mark);
        }
        double height = 0.68 + 0.30 * tier;
        double wide = 0.098 + 0.006 * tier;
        double foot = z;
        b.solid(circ(0, 0, wide), z, height * 0.62, marble);
        z += height * 0.62;
        b.solid(circ(0, 0, wide + 0.022), z, 0.03, marble);
        b.line(circ(0, 0, wide + 0.022), z + 0.03, WHITE_GOLD, 0.010);
        z += 0.03;
        b.solid(circ(0, 0, wide * 0.80), z, height * 0.38, marble);
        z += height * 0.38;
        for (int i = 0; i < 3; i++) {
            double v = foot + height * (0.16 + 0.20 * i);
            b.line(open(0, wide - LIFT * v, 0, wide - LIFT * (v + 0.07)), 0, window, 0.016);
        }
        b.line(open(0, wide * 0.80 - LIFT * (z - height * 0.26), 0, wide * 0.80 - LIFT * (z - height * 0.10)), 0,
                release ? Color.WHITE : WHITE_LIGHT, 0.018);
        double eaves = z;
        double[] cone = {1.12, 0.84, 0.56, 0.30};
        for (double c : cone) {
            b.solid(circ(0, 0, wide * 0.80 * c), z, 0.065, roof);
            z += 0.065;
        }
        b.solid(circ(0, 0, 0.011), z, 0.12, gold);
        z += 0.12;
        if (tier >= 1) {
            b.flat(poly(0, -0.012, 0.085, 0.004, 0, 0.02), z - 0.02, tier == 3 ? WHITE_GOLD : WHITE_ROOF);
        }
        if (release) {
            b.glow(0, 0, 0.28, eaves, alpha(WHITE_LIGHT, 0.8));
        }
        b.light(Life.BREATH, 0, wide * 0.80, eaves - height * 0.18, 0.13, WHITE_LIGHT);
        b.light(Life.GLINT, 0, 0, z, 0.07, Color.WHITE);
        b.top = z;
        b.nest = eaves - 0.04;
        return b;
    }

    // ==== Obelisk (REPLACES TowerFoundations.obelisk and rune) ========================================================

    /** The menhir's plan at its foot: a rough slab, its south edge straight along the rune face. */
    private static Shape menhirPlan() {
        return poly(-0.110, MENHIR_FACE, -0.125, -0.010, -0.075, -0.080, 0.025, -0.095, 0.100, -0.055, 0.125, 0.020,
                0.105, MENHIR_FACE);
    }

    private static final double[] MENHIR_SHARE = {0.34, 0.28, 0.20, 0.11, 0.07};
    private static final double[] MENHIR_TAPER = {1.0, 0.97, 0.92, 0.80, 0.50};
    private static final double[] MENHIR_LEAN = {0, 0.003, 0.007, 0.012, -0.028};

    static double menhirHeight(int tier) {
        return 0.72 + 0.20 * tier;
    }

    /**
     * A menhir: a rough standing stone that narrows and leans as it rises to an uneven top, runes cut into its south
     * face. {@code look} picks the great rune; lesser ones come with the tiers. A knell throws the stone from side to
     * side as its recoil runs out, more at its top than at its foot.
     */
    private static Build obelisk(int tier, TowerPose pose, TowerLook look) {
        Tones dark = new Tones(new Color(0x48446c), new Color(0x6a6490), new Color(0xa39ad0));
        Build b = plinth(tier, colour(TowerLook.PULSE), r -> box(0, 0, r * 1.72, r * 1.72));
        double z = b.top;
        double reach = (b.footprint - PLINTH_STEP * 0.5) * 0.86 - 0.035;
        if (tier == 2) {
            int mark = b.mark();
            for (int i = 0; i < 3; i++) {
                b.solid(at(ngon(0, 0, 5, 0.045, 20 + i * 37), -90 + i * 120, reach * 0.9), STEP_HEIGHT, 0.2, dark);
            }
            b.around(mark);
        }
        if (tier == 3) {
            int mark = b.mark();
            for (int i = 0; i < 8; i++) {
                double a = 22.5 + i * 45;
                double d = reach / Math.max(Math.abs(cos(a)), Math.abs(sin(a))) * 0.93;
                b.solid(mv(ngon(0, 0, 5, 0.042, i * 37), cos(a) * d, sin(a) * d), STEP_HEIGHT, 0.20 + 0.04 * (i % 2),
                        dark);
            }
            b.around(mark);
        }
        double height = menhirHeight(tier);
        double grow = tier == 3 ? 1.12 : 1.0;
        double shake = KNELL_SHAKE[Math.clamp(Math.round(pose.recoil() * 4), 0, 4)];
        double foot = z;
        for (int i = 0; i < MENHIR_SHARE.length; i++) {
            double sway = shake * (i + 1) / MENHIR_SHARE.length;
            Shape slice = scaled(menhirPlan(), MENHIR_TAPER[i] * grow, MENHIR_TAPER[i] * grow, 0, MENHIR_FACE);
            b.solid(mv(slice, MENHIR_LEAN[i] + sway, 0), z, height * MENHIR_SHARE[i], dark);
            z += height * MENHIR_SHARE[i];
        }
        b.top = z;
        runes(b, look, foot, height, tier, pose, shake);
        return b;
    }

    /** Where the face's middle stands at {@code share} of the menhir's height: it leans as the slices do. */
    private static double faceLean(double share) {
        double from = 0;
        for (int i = 0; i < MENHIR_SHARE.length; i++) {
            if (share <= from + MENHIR_SHARE[i]) {
                return MENHIR_LEAN[Math.min(i, 3)];
            }
            from += MENHIR_SHARE[i];
        }
        return 0;
    }

    /**
     * The runes on the face, from the foot up: a lesser rune from Tier I, the great rune of {@code look} at mid
     * height, a lesser rune above it from Tier II, a mark near the top at Tier III. Each is cut in screen measures on
     * the face (a line part at height 0), glows, and carries a light that climbs the stone while it idles. A full
     * store brightens every rune; a knell turns them white.
     */
    private static void runes(Build b, TowerLook look, double foot, double height, int tier, TowerPose pose,
                              double shake) {
        boolean charged = pose.has(TowerCue.CHARGED);
        Color c = switch (look) {
            case BURDEN -> new Color(0xc9a56c);
            case GRAVITY -> new Color(0x7d9be6);
            case UMBRA -> new Color(0xbcaaf0);
            default -> TowerLooks.tierColor(TowerLook.PULSE, tier);
        };
        Color ink = mix(c, Color.WHITE, pose.flash() ? 0.85 : charged ? 0.5 : 0.1);
        double[] at = {0.17, 0.42, 0.66, 0.84};
        boolean[] shown = {tier >= 1, true, tier >= 2, tier >= 3};
        for (int i = 0; i < at.length; i++) {
            if (!shown[i]) {
                continue;
            }
            double sway = shake * at[i];
            double x = faceLean(at[i]) + sway;
            double y = MENHIR_FACE - LIFT * (foot + height * at[i]);
            double size = i == 1 ? 1.0 : 0.62;
            double w = 0.020 * size;
            b.glow(x, y, 0.085 * size, 0, alpha(c, pose.flash() ? 1.0 : charged ? 0.9 : 0.45));
            if (!charged && !pose.flash()) {
                b.light(Life.RUNE, x, y, 0, 0.11 * size, c);
            }
            int first = b.mark();
            if (i == 1) {
                greatRune(b, look, ink, w);
            } else if (i == 0) {
                b.line(open(-0.035, -0.04, 0.02, 0, -0.035, 0.04), 0, ink, w);
            } else if (i == 2) {
                b.line(open(0, -0.045, 0, 0.045), 0, ink, w);
                b.line(open(-0.035, -0.045, 0, -0.005, 0.035, -0.045), 0, ink, w);
            } else {
                b.line(open(-0.03, 0, 0.03, 0), 0, ink, w);
                b.flat(circ(0, -0.035, 0.012), 0, ink);
            }
            AffineTransform place = AffineTransform.getTranslateInstance(x, y);
            place.scale(size, size * RUNE_SQUASH);
            for (int k = first; k < b.parts.size(); k++) {
                b.parts.set(k, b.parts.get(k).withShape(place.createTransformedShape(b.parts.get(k).shape())));
            }
        }
    }

    /** The great rune, drawn about 0,0 before it is placed: the shapes the stone's top carried, a little larger. */
    private static void greatRune(Build b, TowerLook look, Color ink, double w) {
        switch (look) {
            case BURDEN -> {
                b.line(open(-0.06, -0.052, 0.06, -0.052), 0, ink, w);
                b.line(open(-0.052, -0.006, 0, 0.052, 0.052, -0.006), 0, ink, w);
            }
            case GRAVITY -> {
                Path2D.Double spiral = new Path2D.Double();
                for (int i = 0; i <= 24; i++) {
                    double a = i * 30;
                    double r = 0.066 - 0.058 * i / 24.0;
                    if (i == 0) {
                        spiral.moveTo(cos(a) * r, sin(a) * r);
                    } else {
                        spiral.lineTo(cos(a) * r, sin(a) * r);
                    }
                }
                b.line(spiral, 0, ink, w * 0.8);
            }
            case UMBRA -> {
                b.line(circ(0, 0, 0.054), 0, ink, w);
                b.flat(circ(0.016, 0.016, 0.046), 0, new Color(0x2a2540));
            }
            default -> {
                b.line(open(0, -0.06, 0, 0.06), 0, ink, w);
                b.line(open(-0.04, -0.016, 0.04, -0.016), 0, ink, w);
            }
        }
    }

    /**
     * The menhir's shadow lying on the ground along +X from the stone's foot, {@code reach} cells long: the stone's own
     * outline, slice by slice, with its uneven top at the far end. The Sundial turns this instead of a wedge.
     */
    /** The shadow is this much wider than the stone at its foot, and this much wider again by its tip. */
    static final double SHADOW_WIDTH = 1.7;
    static final double SHADOW_SPREAD = 0.9;

    static Shape menhirShadow(int tier, double reach) {
        double grow = tier == 3 ? 1.12 : 1.0;
        Path2D.Double p = new Path2D.Double();
        double[] left = new double[MENHIR_SHARE.length];
        double[] right = new double[MENHIR_SHARE.length];
        for (int i = 0; i < MENHIR_SHARE.length; i++) {
            double spread = SHADOW_WIDTH * (1 + SHADOW_SPREAD * i / (MENHIR_SHARE.length - 1.0));
            left[i] = (-0.125 * MENHIR_TAPER[i] + MENHIR_LEAN[i]) * grow * spread;
            right[i] = (0.125 * MENHIR_TAPER[i] + MENHIR_LEAN[i]) * grow * spread;
        }
        double along = 0;
        p.moveTo(0, left[0]);
        for (int i = 0; i < MENHIR_SHARE.length; i++) {
            p.lineTo(along * reach, left[i]);
            along += MENHIR_SHARE[i];
            p.lineTo(along * reach, left[i]);
        }
        p.lineTo(reach, (left[4] + right[4]) / 2 + 0.01);
        for (int i = MENHIR_SHARE.length - 1; i >= 0; i--) {
            p.lineTo(along * reach - (i == 4 ? reach * 0.03 : 0), right[i]);
            along -= MENHIR_SHARE[i];
            p.lineTo(along * reach, right[i]);
        }
        p.closePath();
        return p;
    }
}
