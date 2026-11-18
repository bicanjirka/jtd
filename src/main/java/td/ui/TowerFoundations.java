package td.ui;

import td.ui.render.LoadedShell;
import td.ui.render.TowerCue;
import td.ui.render.TowerLook;
import td.ui.render.TowerPose;
import td.util.ThreadConfined;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.DoubleFunction;

/**
 * The table of tower foundations: one row per {@link TowerLook}, checked complete when this class loads. A tower is a
 * stack of flat parts in cell units (the cell's centre at 0,0, a cell 1 wide, north up), each with a height. Its body
 * never turns and grows a step with every tier; a look that aims also has a head, built at the body's top along
 * {@code +X}. What a tower holds or has just done is its {@link TowerPose}, and every part that shows it is built here
 * from the pose, so the picture of a moment is the same wherever it is drawn.
 */
final class TowerFoundations {

    /** The tiers a foundation is drawn at; a tier above the last wears the last's. */
    static final int TIERS = 4;
    /** A pose's recoil and reload are built at this many even steps, so the pictures kept of them stay few. */
    static final int POSE_STEPS = 4;
    /** How far round, in cells, a shell lies in a barrel's bore: the size every other drawing of a shell scales from. */
    static final double BORE_SHELL = 0.034;

    private static final double[] FOOTPRINT = {0.275, 0.315, 0.36, 0.40};
    private static final double PLINTH_STEP = 0.045;
    private static final double STEP_HEIGHT = 0.07;
    /** How far up the screen a part's top is lifted, as a share of its height: the board is seen from the south. */
    static final double LIFT = 0.42;
    private static final Color PLINTH_STONE = new Color(0x50535e);
    private static final Color PAVING_DARK = new Color(0x101014);
    private static final Color GUNMETAL = new Color(0x59626a);
    private static final Color SHADE_STONE = new Color(0x5a6470);
    private static final Color SHADE_LIGHT = new Color(0xa6e8c8);
    private static final Color SOUL_STONE = new Color(0x7a54d8);
    private static final Color SOUL_FLAME = new Color(0xb9a0ff);
    private static final Color LANTERN_LIGHT = new Color(0xffe9a8);
    private static final Color DAYLIGHT = new Color(0xfffdf0);
    private static final Color WHITE_TOWER = new Color(0xe6eaf2);
    private static final Color WHITE_LIGHT = new Color(0xfff4d6);
    private static final Color WHITE_ROOF = new Color(0x8fa9d6);
    private static final Color WHITE_GOLD = new Color(0xe8cf8a);
    private static final Color RUBY_EMBER = new Color(0xff7a5a);
    /** The plain Aerie's foot: a dark disc round the spire's foot, so the pale crystal never melts into its base. */
    private static final Color AERIE_FOOT = new Color(0x0b1a2c);
    /** The foot's radius, a share of the body's. */
    private static final double AERIE_FOOT_SHARE = 0.64;
    /** The plane of the menhir's rune face, south of the stone's centre, in cells. */
    private static final double MENHIR_FACE = 0.085;
    /** How far the menhir is thrown sideways at each step of a knell's recoil, from at rest to just rung, in cells. */
    private static final double[] KNELL_SHAKE = {0, -0.005, 0.009, -0.013, 0.016};
    /**
     * How tall a rune is drawn on the face against its width: the face is seen from above, and a rune foreshortened as
     * its wall is would not read.
     */
    private static final double RUNE_SQUASH = 0.75;
    /** Where each of a discharge's sparks leaves the crown, in degrees. */
    private static final double[] SPARK_BEARINGS = {36, 124, 215, 294};
    /** Past this many built poses every one is dropped and built again as it is needed. */
    private static final int KEPT_POSES = 2048;
    /** Steady Aim from this many stacks shows as focused. */
    static final int FOCUS_STACKS = 5;
    static final Color FOCUS_GREEN = new Color(0x9dffb0);
    static final Color KEEN_GOLD = new Color(0xffd36a);
    /**
     * Frenzy throws the head about: one of these offsets, in cells (along the barrel, across it), a step of the tick
     * clock while Frenzy runs; the head is drawn at each, so the shake costs four kept pictures a heading.
     */
    static final double[][] FRENZY_SHAKE = {{0, 0.014}, {-0.012, -0.010}, {0.008, -0.016}, {-0.006, 0.012}};
    static final int FRENZY_STEPS = FRENZY_SHAKE.length;
    /** Frenzy's shake moves to its next step this many ticks apart. */
    static final int FRENZY_SHAKE_TICKS = 2;
    /** How far the thrower is turned from north, clockwise in degrees: it throws over the north-east. */
    static final double THROWER_TURN = 35;
    /**
     * The arm: where it pivots on the skein (south of the cell's centre, along the machine), and its angles in degrees
     * above the ground, from pointing back: cocked, being winched down, and slammed into the pad.
     */
    static final double THROWER_PIVOT = 0.05;
    static final double THROWER_COCKED = 24;
    static final double THROWER_WINDING = 75;
    static final double THROWER_THROWN = 123;
    /**
     * The radius, in cells, of every shell the Mortar shows on its tower: racked, carried and loaded. Smaller than the
     * shell in flight on purpose, so the rack fits the cell and the barrel stays in proportion.
     */
    static final double MORTAR_SHELL = 0.056;
    /** Where the bedplate stands on its cell (its centre), and how far north the barrel leans per cell up. */
    static final double MORTAR_X = 0.0;
    static final double MORTAR_Y = -0.07;
    static final double MORTAR_LEAN = 0.30;
    /** The barrel: its slices' height, how many (one more with a long barrel), its bore and its muzzle ring. */
    static final double MORTAR_SLICE = 0.066;
    static final int MORTAR_SLICES = 6;
    static final double MORTAR_BORE = 0.085;
    static final double MORTAR_MUZZLE = 0.135;
    /** How far a shot throws the barrel down its own axis, in cells. */
    static final double MORTAR_RECOIL = 0.04;
    /** The ready rack in front of the mortar: its row's distance south of the cell's centre, and its shells' spacing. */
    static final double MORTAR_RACK_Y = 0.15;
    /** The rack stands on a crate this tall, so its shells show over the deck's edge. */
    static final double MORTAR_RACK_HEIGHT = 0.07;
    static final double MORTAR_RACK_GAP = MORTAR_SHELL * 2.1;
    /** The rack holds one shell of each kind the tower can load: at most this many on the bottom row, the rest on top. */
    static final int MORTAR_RACK_ROW = 3;
    /** The plank deck the mortar stands on from Tier I: its thickness. */
    static final double MORTAR_DECK = 0.025;
    /** The menhir's slices from its foot up: each one's share of the stone's height, its width and its lean. */
    private static final double[] MENHIR_SHARE = {0.34, 0.28, 0.20, 0.11, 0.07};
    private static final double[] MENHIR_TAPER = {1.0, 0.97, 0.92, 0.80, 0.50};
    private static final double[] MENHIR_LEAN = {0, 0.003, 0.007, 0.012, -0.028};

    private static final Map<TowerLook, Row> ROWS = checkedComplete(rows());
    private static final Map<TowerLook, List<Foundation>> IDLE = idle();
    private static final Map<Posed, Foundation> POSED = new ConcurrentHashMap<>();

    private TowerFoundations() {
    }

    enum Kind {
        /** A closed shape with a height: its side, a lit rim along its top-left edge, its top. */
        SOLID,
        /** A flat detail lying on what is under it. */
        FLAT,
        /** A soft light that fades out from its centre. */
        GLOW,
        /** A thin stroke. */
        LINE
    }

    /** How a light lives while its tower only stands there. */
    enum Life {
        /** A soft glow that swells and fades slowly. */
        BREATH,
        /** A small flame that never holds still. */
        FLICKER,
        /** A four-point star that flashes once in a while. */
        GLINT,
        /** A pale ray that turns round its centre. */
        SWEEP
    }

    /** One part: a shape whose foot stands {@code z0} up, {@code height} tall, in its three tones or one colour. */
    record Part(Kind kind, Shape shape, double z0, double height, Tones tones, Color color, double width) {

        static Part solid(Shape shape, double z0, double height, Tones tones) {
            return new Part(Kind.SOLID, shape, z0, height, tones, null, 0);
        }

        static Part flat(Kind kind, Shape shape, double z, Color color) {
            return new Part(kind, shape, z, 0, null, color, 0);
        }

        static Part line(Shape shape, double z, Color color, double width) {
            return new Part(Kind.LINE, shape, z, 0, null, color, width);
        }

        Part withShape(Shape shape) {
            return new Part(this.kind, shape, this.z0, this.height, this.tones, this.color, this.width);
        }
    }

    /**
     * A light on a tower that stays alive while the tower is idle: where it sits ({@code z} up), how far it reaches and
     * how it lives. One on a head turns with it.
     */
    record Light(Life life, double x, double y, double z, double radius, Color color, boolean turning) {

        static Light of(Life life, double x, double y, double z, double radius, Color color) {
            return new Light(life, x, y, z, radius, color, false);
        }

        /** The same light on a head. */
        Light asTurning() {
            return new Light(this.life, this.x, this.y, this.z, this.radius, this.color, true);
        }
    }

    /**
     * A tower built for one tier and pose. {@code parts} are its body in paint order, back to front, and {@code head}
     * the parts it aims, along {@code +X}, already standing at {@code top}; empty for a look that does not aim.
     * {@code front} is what of an aiming look's body stands south of its centre: it is painted over the head, so a
     * barrel turned south passes behind a post or a wall that stands nearer. {@code footprint} is the tower's radius
     * on the ground, {@code nest} how high a ring round it circles, and {@code muzzle} and {@code muzzleHeight} where
     * a shot leaves it: how far out along its aim and how high.
     */
    record Foundation(List<Part> parts, List<Part> head, List<Part> front, List<Light> lights, double top,
                      double footprint, double nest, double muzzle, double muzzleHeight) {

        Foundation {
            parts = List.copyOf(parts);
            head = List.copyOf(head);
            front = List.copyOf(front);
            lights = List.copyOf(lights);
        }

        /** What {@code build} came to; its muzzle is the far end of the head's furthest solid part. */
        static Foundation of(Build build) {
            List<Part> front = build.head.isEmpty() ? List.of() : List.copyOf(build.front);
            if (build.head.isEmpty()) {
                build.finish();
            }
            double muzzle = 0;
            double height = build.top;
            for (Part part : build.head) {
                double far = part.shape().getBounds2D().getMaxX();
                if (part.kind() == Kind.SOLID && far > muzzle) {
                    muzzle = far;
                    height = part.z0() + part.height() / 2;
                }
            }
            return new Foundation(build.parts, build.head, front, build.lights, build.top, build.footprint, build.nest,
                    muzzle, height);
        }

        boolean aims() {
            return !this.head.isEmpty();
        }
    }

    /** Builds a look at a tier in a pose. */
    @FunctionalInterface
    private interface Builder {
        Build at(int tier, TowerPose pose);
    }

    /** A look's builder; {@code stillBody} when its body is the same in every pose, so only its head is built again. */
    private record Row(Builder builder, boolean stillBody) {

        static Row aiming(Builder builder) {
            return new Row(builder, true);
        }

        static Row posed(Builder builder) {
            return new Row(builder, false);
        }
    }

    private record Posed(TowerLook look, int tier, TowerPose pose) {
    }

    /** {@code look} at {@code tier}, at rest. */
    static Foundation of(TowerLook look, int tier) {
        return IDLE.get(look).get(Math.clamp(tier, 0, TIERS - 1));
    }

    /** {@code look} at {@code tier} in {@code pose}. */
    static Foundation of(TowerLook look, int tier, TowerPose pose) {
        TowerPose snapped = pose.snapped(POSE_STEPS);
        int shown = Math.clamp(tier, 0, TIERS - 1);
        if (snapped.equals(TowerPose.idle())) {
            return IDLE.get(look).get(shown);
        }
        Posed key = new Posed(look, shown, snapped);
        Foundation kept = POSED.get(key);
        if (kept != null) {
            return kept;
        }
        if (POSED.size() > KEPT_POSES) {
            POSED.clear();
        }
        Foundation built = Foundation.of(ROWS.get(look).builder().at(shown, snapped));
        POSED.putIfAbsent(key, built);
        return built;
    }

    /** Whether {@code look}'s body is the same in every pose. */
    static boolean stillBody(TowerLook look) {
        return ROWS.get(look).stillBody();
    }

    /** The radius a tower of {@code tier} takes on the ground, in cells. */
    static double footprint(int tier) {
        return FOOTPRINT[Math.clamp(tier, 0, TIERS - 1)];
    }

    static <V> Map<TowerLook, V> checkedComplete(Map<TowerLook, V> rows) {
        Set<TowerLook> missing = EnumSet.allOf(TowerLook.class);
        missing.removeAll(rows.keySet());
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Tower looks without a foundation: " + missing);
        }
        return rows;
    }

    private static Map<TowerLook, List<Foundation>> idle() {
        Map<TowerLook, List<Foundation>> built = new EnumMap<>(TowerLook.class);
        ROWS.forEach((look, row) -> {
            List<Foundation> tiers = new ArrayList<>(TIERS);
            for (int tier = 0; tier < TIERS; tier++) {
                tiers.add(Foundation.of(row.builder().at(tier, TowerPose.idle())));
            }
            built.put(look, List.copyOf(tiers));
        });
        return built;
    }

    private static Map<TowerLook, Row> rows() {
        Map<TowerLook, Row> rows = new EnumMap<>(TowerLook.class);
        rows.put(TowerLook.SNIPER, Row.aiming(TowerFoundations::sniper));
        rows.put(TowerLook.SPLASH, Row.posed(TowerFoundations::thrower));
        rows.put(TowerLook.STORMCALLER, Row.posed((tier, pose) -> stormcaller(Math.max(1, tier), pose)));
        rows.put(TowerLook.HEXER, Row.posed((tier, pose) -> hexer(Math.max(1, tier), pose)));
        rows.put(TowerLook.MORTAR, Row.posed(TowerFoundations::siegeMortar));
        rows.put(TowerLook.CINDER, Row.posed((tier, pose) -> scorcher(tier, pose, false)));
        rows.put(TowerLook.SOULFIRE, Row.posed((tier, pose) -> scorcher(tier, pose, true)));
        rows.put(TowerLook.SEEKER, Row.posed(TowerFoundations::aerie));
        rows.put(TowerLook.RUBY, Row.posed(TowerFoundations::ruby));
        rows.put(TowerLook.SHADE, Row.posed(TowerFoundations::shade));
        rows.put(TowerLook.WHITE, Row.posed(TowerFoundations::white));
        rows.put(TowerLook.PULSE, Row.posed((tier, pose) -> obelisk(tier, pose, TowerLook.PULSE)));
        rows.put(TowerLook.BURDEN, Row.posed((tier, pose) -> obelisk(tier, pose, TowerLook.BURDEN)));
        rows.put(TowerLook.GRAVITY, Row.posed((tier, pose) -> obelisk(tier, pose, TowerLook.GRAVITY)));
        rows.put(TowerLook.UMBRA, Row.posed((tier, pose) -> obelisk(tier, pose, TowerLook.UMBRA)));
        rows.put(TowerLook.AURA, Row.posed(TowerFoundations::beacon));
        return rows;
    }

    // ---- colour ----------------------------------------------------------------------------------------------------

    static Color mix(Color a, Color b, double share) {
        return new Color((int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * share),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * share),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * share));
    }

    private static Color alpha(Color c, double share) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) Math.round(Math.clamp(share, 0, 1) * 255));
    }

    private static Tones tones(int rgb) {
        return Tones.of(new Color(rgb));
    }

    /** A plinth's stone: one grey, leaning a little toward the tower's colour. */
    private static Tones stone(Color tower) {
        return Tones.of(mix(PLINTH_STONE, tower, 0.24));
    }

    private static Color colour(TowerLook look) {
        return TowerLooks.color(look);
    }

    // ---- shapes, in cell units -------------------------------------------------------------------------------------

    static Shape circ(double x, double y, double r) {
        return new Ellipse2D.Double(x - r, y - r, r * 2, r * 2);
    }

    private static Shape ell(double x, double y, double rx, double ry) {
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

    static Shape poly(double... xy) {
        Path2D.Double p = new Path2D.Double();
        p.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) {
            p.lineTo(xy[i], xy[i + 1]);
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

    /** {@code s} turned {@code deg} and moved {@code r} out along that bearing. */
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

    // ---- a foundation under construction ---------------------------------------------------------------------------

    /** Parts from the ground up, in paint order. */
    @ThreadConfined(ThreadConfined.Owner.ENCLOSING)
    private static final class Build {

        private final List<Part> parts = new ArrayList<>();
        /** What stands south of the tower's centre: painted last, over everything behind it. */
        private final List<Part> front = new ArrayList<>();
        private final List<Part> head = new ArrayList<>();
        private final List<Light> lights = new ArrayList<>();
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

        /** A light that lives while the tower idles, {@code r} across at its fullest. */
        Build light(Life life, double x, double y, double z, double r, Color c) {
            this.lights.add(Light.of(life, x, y, z, r, c));
            return this;
        }

        /** {@code turning}'s parts and lights become this tower's head: what it aims, built along {@code +X}. */
        Build aim(Build turning) {
            this.head.addAll(turning.parts);
            for (Light light : turning.lights) {
                this.lights.add(light.asTurning());
            }
            return this;
        }

        int mark() {
            return this.parts.size();
        }

        /**
         * The parts added since {@code from} stand around the tower's centre: those north of it stay behind the centre,
         * those south of it are painted last, and one that straddles it is cut in two.
         */
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

        /**
         * As {@link #around}, for a ring round a column: the ring's far half goes in under the column's parts, which
         * start at {@code column}, so the column hides it; its near half stands in front.
         */
        Build aroundColumn(int column, int from) {
            this.around(from);
            List<Part> far = new ArrayList<>(this.parts.subList(from, this.parts.size()));
            this.parts.subList(from, this.parts.size()).clear();
            this.parts.addAll(column, far);
            return this;
        }

        Build finish() {
            this.parts.addAll(this.front);
            this.front.clear();
            return this;
        }
    }

    /** The cue every tower shares: one plinth step per tier, in the tower's own plan shape, its top step paved. */
    private static Build plinth(int tier, Color tower, DoubleFunction<Shape> shape) {
        Build b = new Build();
        Tones stone = stone(tower);
        double r = FOOTPRINT[tier];
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

    // ---- shared helpers ---------------------------------------------------------------------------------------------

    /** {@code s} scaled by {@code sx, sy} about {@code ax, ay}. */
    static Shape scaled(Shape s, double sx, double sy, double ax, double ay) {
        AffineTransform t = AffineTransform.getTranslateInstance(ax, ay);
        t.scale(sx, sy);
        t.translate(-ax, -ay);
        return t.createTransformedShape(s);
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

    // ---- Sniper: a watchtower of triangles ---------------------------------------------------------------------------

    private static Build sniper(int tier, TowerPose pose) {
        Color green = colour(TowerLook.SNIPER);
        Tones body = Tones.of(green);
        Color deck = mix(green, new Color(0x16241a), 0.66);
        Tones under = Tones.of(mix(green, new Color(0x16241a), 0.38));
        Build b = plinth(tier, green, r -> ngon(0, 0, 6, r, 0));
        double r = b.bodyRadius;
        double z = b.top;
        if (tier >= 2) {
            b.solid(ngon(0, 0, 3, r, 90), z, 0.10, under);
            z += 0.10;
        }
        b.solid(ngon(0, 0, 3, r, -90), z, 0.16, body);
        z += 0.16;
        b.flat(ngon(0, 0.005, 3, r * 0.62, -90), z, deck);
        Tones post = Tones.of(body.highlight());
        if (tier >= 1) {
            int mark = b.mark();
            for (int i = 0; i < 3; i++) {
                b.solid(at(circ(0, 0, 0.04), -90 + 120 * i, r * 0.86), z, 0.10, post);
            }
            b.around(mark);
        }
        if (tier >= 3) {
            int mark = b.mark();
            for (int i = 0; i < 3; i++) {
                b.solid(at(circ(0, 0, 0.036), 90 + 120 * i, r * 0.86), z - 0.16, 0.14, post);
            }
            b.around(mark);
            b.solid(ngon(0, 0, 6, r * 0.40, 0), z, 0.10, body);
            z += 0.10;
            b.flat(ngon(0, 0, 6, r * 0.27, 0), z, deck);
        }
        b.top = z;
        return b.aim(sniperHead(z, pose));
    }

    /**
     * The rifle on its swivel, a scope over it, as the game's, with what it now shows: the lens brightens with each
     * Steady Aim stack up to a star; from {@link #FOCUS_STACKS} the scope is focused (a green halo, sight marks either
     * side, a breathing green light at the muzzle); at full stacks under Keen Eye the lens is a golden eye with long
     * rays and a gold line down the barrel. A loaded Fourth Shot burns red-white at the muzzle. Frenzy shakes the whole
     * head by the pose's beat and throws red sparks off it; the embers rising off it are painted live.
     */
    private static Build sniperHead(double z, TowerPose pose) {
        Color green = colour(TowerLook.SNIPER);
        Tones steel = Tones.of(mix(green, Color.WHITE, 0.55));
        Tones gunmetal = Tones.of(GUNMETAL);
        boolean frenzy = pose.has(TowerCue.FRENZY);
        double sx = frenzy ? FRENZY_SHAKE[Math.floorMod(pose.beat(), FRENZY_SHAKE.length)][0] : 0;
        double sy = frenzy ? FRENZY_SHAKE[Math.floorMod(pose.beat(), FRENZY_SHAKE.length)][1] : 0;
        double len = pose.has(TowerCue.LONG_BARREL) ? 0.37 : 0.30;
        double back = pose.recoil() * 0.05 - sx;
        double lensX = 0.09 - back;
        double lensY = -0.05 + sy;
        int stacks = pose.stacks();
        boolean focused = stacks >= FOCUS_STACKS;
        boolean keen = pose.has(TowerCue.KEEN_FOCUS);
        Build h = new Build();
        h.solid(circ(sx, sy, 0.058), z, 0.03, gunmetal);
        h.solid(box((0.04 + len) / 2 - back, sy, len - 0.04, 0.034), z + 0.03, 0.03, steel);
        if (keen) {
            h.line(open(0.06 - back, sy, len - 0.02 - back, sy), z + 0.06, KEEN_GOLD, 0.008);
        }
        if (pose.has(TowerCue.SILVER_ROUND)) {
            h.solid(box(len - 0.05 - back, sy, 0.10, 0.04), z + 0.03, 0.034, tones(0xaad2ff));
            h.flat(circ(len - 0.03 - back, sy - 0.006, 0.012), z + 0.07, Color.WHITE);
        }
        if (pose.has(TowerCue.HOLLOW_POINT)) {
            h.solid(box(len * 0.55 - back, sy, 0.03, 0.058), z + 0.03, 0.036, tones(0xe69646));
        }
        h.solid(box(-0.015 - back, sy, 0.13, 0.072), z + 0.03, 0.04, Tones.of(green));
        h.solid(box(0.035 - back, lensY, 0.10, 0.028), z + 0.05, 0.03, gunmetal);
        if (focused && !keen) {
            h.glow(lensX, lensY, 0.16, z + 0.08, alpha(FOCUS_GREEN, 0.9));
            h.line(circ(lensX, lensY, 0.055), z + 0.08, FOCUS_GREEN, 0.010);
            h.line(open(lensX - 0.07, lensY - 0.035, lensX - 0.07, lensY + 0.035), z + 0.08, FOCUS_GREEN, 0.008);
            h.line(open(lensX + 0.07, lensY - 0.035, lensX + 0.07, lensY + 0.035), z + 0.08, FOCUS_GREEN, 0.008);
            h.light(Life.BREATH, len - back, sy, z + 0.06, 0.07, FOCUS_GREEN);
        }
        if (keen) {
            h.glow(lensX, lensY, 0.20, z + 0.08, alpha(KEEN_GOLD, 0.95));
            h.line(circ(lensX, lensY, 0.06), z + 0.08, KEEN_GOLD, 0.012);
            h.flat(star(lensX, lensY, 4, 0.085, 0.010, 0), z + 0.08, mix(KEEN_GOLD, Color.WHITE, 0.4));
            h.flat(star(lensX, lensY, 4, 0.05, 0.008, 45), z + 0.08, KEEN_GOLD);
            h.flat(circ(lensX, lensY, 0.022), z + 0.08, KEEN_GOLD);
            h.flat(ell(lensX, lensY, 0.006, 0.016), z + 0.08, new Color(0x2a1a04));
            h.light(Life.BREATH, lensX, lensY, z + 0.08, 0.16, KEEN_GOLD);
        } else {
            int lit = Math.min(4, stacks);
            h.flat(circ(lensX, lensY, 0.017), z + 0.08,
                    mix(new Color(0x2c4030), new Color(0xf2fff0), 0.3 + 0.175 * lit));
            if (stacks >= 3) {
                double size = focused ? 0.06 : 0.042;
                h.glow(lensX, lensY, 0.06, z + 0.08, alpha(new Color(0xf2fff0), 0.8));
                h.flat(star(lensX, lensY, 4, size, 0.008, 0), z + 0.08, Color.WHITE);
            } else {
                h.light(Life.GLINT, lensX, lensY, z + 0.08, 0.075, Color.WHITE);
            }
        }
        if (pose.has(TowerCue.HEADHUNTER)) {
            Color hunt = new Color(0xff7850);
            h.line(circ(lensX, lensY, 0.04), z + 0.08, hunt, 0.014);
            h.line(open(lensX - 0.06, lensY, lensX + 0.06, lensY), z + 0.08, hunt, 0.01);
        }
        if (pose.has(TowerCue.FOURTH_SHOT)) {
            h.glow(len - back, sy, 0.15, z + 0.07, alpha(new Color(0xff5a5a), 0.75));
            h.glow(len - back, sy, 0.075, z + 0.07, alpha(Color.WHITE, 0.95));
            h.flat(ngon(len - back, sy, 4, 0.034, 0), z + 0.07, Color.WHITE);
        }
        if (frenzy) {
            Color spark = new Color(0xff4a3a);
            h.glow(-back, sy, 0.13, z + 0.07, alpha(spark, 0.45));
            h.line(open(-0.07 + sx, -0.05 + sy, -0.13 + sx, -0.10 + sy), z + 0.08, spark, 0.016);
            h.line(open(-0.08 + sx, 0.04 + sy, -0.15 + sx, 0.08 + sy), z + 0.08, spark, 0.016);
            h.line(open(sx, 0.055 + sy, -0.02 + sx, 0.115 + sy), z + 0.08, spark, 0.016);
        }
        if (pose.flash()) {
            h.glow(len + 0.02 - back + 0.05 * pose.recoil(), sy, 0.09, z + 0.07, alpha(new Color(0xf4ffe8), 0.9));
        }
        return h;
    }

    // ---- Burst: the onager ------------------------------------------------------------------------------------------

    /** A shape drawn along the machine (north is its front) turned to where the machine faces. */
    private static Shape turned(Shape s) {
        return rot(s, THROWER_TURN);
    }

    /** A point along the machine ({@code u} across it, {@code v} back from its centre) on the plan. */
    private static double[] turned(double u, double v) {
        double a = Math.toRadians(THROWER_TURN);
        return new double[] {u * Math.cos(a) - v * Math.sin(a), u * Math.sin(a) + v * Math.cos(a)};
    }

    /** A beam in the air from one point of the machine to another, a shaded line drawn in screen measures. */
    private static void beam(Build b, double u0, double v0, double z0, double u1, double v1, double z1, Tones t,
                             double w) {
        double[] p = turned(u0, v0);
        double[] q = turned(u1, v1);
        b.line(open(p[0], p[1] - LIFT * z0, q[0], q[1] - LIFT * z1), 0, t.shade(), w);
        b.line(open(p[0] - w * 0.18, p[1] - LIFT * z0 - w * 0.18, q[0] - w * 0.18, q[1] - LIFT * z1 - w * 0.18), 0,
                t.highlight(), w * 0.38);
    }

    /**
     * A wooden onager, the cheap stone thrower a village builds, set across its cell to throw over the north-east: two
     * rails on a plank bed, a twisted rope skein between them with a winch at each end, a tall stop frame at the front
     * with a padded crossbar, and one arm with a cup. Cocked, the arm lies back with a stone in its cup; at the throw it
     * stands slammed into the pad; while it reloads it is winched back down and a stone is set in the cup. It never
     * turns: it throws over its front at whatever it picked. The tiers grow it and never ring it: iron bands and
     * spoked winches, then side braces and iron caps, then a banner. A basket of stones beside it, a heap from Tier II.
     */
    private static Build thrower(int tier, TowerPose pose) {
        Color burst = colour(TowerLook.SPLASH);
        Tones wood = Tones.of(burst);
        Tones dark = Tones.of(mix(wood.shade(), new Color(0x2a1a16), 0.25));
        Tones plank = Tones.of(mix(wood.shade(), new Color(0x2a1f1c), 0.45));
        Tones arm = Tones.of(mix(burst, new Color(0xe8c8a0), 0.25));
        Tones iron = tones(0x5c5450);
        Tones rope = tones(0xa8916a);
        Tones wicker = tones(0x6e5030);
        Tones rock = tones(0xd8c8c0);
        Tones pad = tones(0xb8a07a);
        Build b = plinth(tier, burst, r -> circ(0, 0, r));
        double z = b.top;
        b.solid(turned(box(0, 0.01, 0.30, 0.42)), z, 0.03, plank);
        z += 0.03;
        for (int i = -1; i <= 1; i += 2) {
            b.flat(turned(box(i * 0.05, 0.01, 0.006, 0.40)), z, plank.shade());
        }
        if (tier >= 1) {
            for (int i = -1; i <= 1; i += 2) {
                b.flat(turned(box(i * 0.143, 0.01, 0.014, 0.42)), z, iron.body());
            }
        }
        double rail = 0.095;
        double frameV = -0.10;
        double frameTop = z + 0.30 + 0.05 * tier;
        double skein = z + 0.035;
        b.solid(turned(box(0, -0.16, rail * 2 + 0.02, 0.04)), z, 0.045, dark);
        for (int s = -1; s <= 1; s += 2) {
            b.solid(turned(box(s * rail, 0.02, 0.045, 0.40)), z, 0.05, dark);
            if (tier >= 1) {
                for (double v : new double[] {-0.04, 0.15}) {
                    b.solid(turned(box(s * rail, v, 0.05, 0.016)), z + 0.05, 0.004, iron);
                }
            }
        }
        for (int s = -1; s <= 1; s += 2) {
            b.solid(turned(box(s * rail, frameV, 0.048, 0.048)), z + 0.05, frameTop - z - 0.05, wood);
        }
        if (tier >= 2) {
            for (int s = -1; s <= 1; s += 2) {
                beam(b, s * rail, 0.12, z + 0.05, s * rail, frameV + 0.02, frameTop - 0.04, dark, 0.03);
            }
        }
        b.solid(turned(box(0, frameV, rail * 2 + 0.08, 0.05)), frameTop, 0.045, wood);
        if (tier >= 2) {
            for (int s = -1; s <= 1; s += 2) {
                b.solid(turned(box(s * (rail + 0.04), frameV, 0.03, 0.056)), frameTop, 0.05, iron);
            }
        }
        b.solid(turned(box(0, frameV + 0.035, 0.12 + 0.01 * tier, 0.03)), frameTop - 0.05, 0.08, pad);
        b.solid(turned(box(0, 0.20, rail * 2 + 0.02, 0.04)), z, 0.045, dark);
        b.solid(turned(box(0, THROWER_PIVOT, rail * 2 - 0.04, 0.065)), skein, 0.055, rope);
        for (int i = 0; i < 4; i++) {
            b.flat(turned(box(-0.054 + i * 0.036, THROWER_PIVOT, 0.008, 0.065)), skein + 0.055, rope.shade());
        }
        for (int s = -1; s <= 1; s += 2) {
            double wu = s * (rail + 0.04);
            double size = tier >= 1 ? 0.11 : 0.08;
            b.solid(turned(box(wu, THROWER_PIVOT, 0.024, size)), z, size, iron);
            if (tier >= 1) {
                double[] c = turned(wu, THROWER_PIVOT);
                double cy = c[1] - LIFT * (z + size / 2);
                b.line(open(c[0] - 0.03, cy - 0.03, c[0] + 0.03, cy + 0.03), 0, iron.highlight(), 0.008);
                b.line(open(c[0] - 0.03, cy + 0.03, c[0] + 0.03, cy - 0.03), 0, iron.highlight(), 0.008);
            }
        }
        double pivotZ = skein + 0.055;
        double length = 0.22 + 0.015 * tier;
        double angle = pose.flash() ? THROWER_THROWN : pose.reload() < 0.25 ? THROWER_WINDING : THROWER_COCKED;
        double a = Math.toRadians(angle);
        double endV = THROWER_PIVOT + Math.cos(a) * length;
        double endZ = pivotZ + Math.sin(a) * length;
        beam(b, 0, THROWER_PIVOT, pivotZ, 0, endV, endZ, arm, 0.05 + 0.004 * tier);
        if (tier >= 1) {
            double t = 0.45;
            double[] m = turned(0, THROWER_PIVOT + (endV - THROWER_PIVOT) * t);
            double mz = pivotZ + (endZ - pivotZ) * t;
            b.solid(circ(m[0], m[1], 0.03), mz - 0.01, 0.02, iron);
        }
        double cup = 0.048 + 0.003 * tier;
        double[] e = turned(0, endV);
        b.solid(circ(e[0], e[1], cup), endZ - 0.02, 0.035, dark);
        b.flat(circ(e[0], e[1], cup * 0.68), endZ + 0.015, plank.shade());
        if (!pose.flash() && pose.reload() > 0.5) {
            b.solid(ngon(e[0], e[1], 6, cup * 0.74, 10), endZ + 0.01, 0.04, rock);
        }
        int basket = b.mark();
        double[] k = turned(-0.18, 0.12);
        b.solid(circ(k[0], k[1], 0.05), z - 0.03, 0.06, wicker);
        b.flat(ring(k[0], k[1], 0.05, 0.04), z + 0.03, wicker.highlight());
        for (int i = 0; i < 3; i++) {
            b.solid(mv(at(ngon(0, 0, 6, 0.022, i * 23), 30 + i * 120, 0.02), k[0], k[1]), z + 0.02, 0.03, rock);
        }
        b.around(basket);
        if (tier >= 2) {
            int heap = b.mark();
            for (int i = 0; i < 3; i++) {
                double[] h = turned(0.17 - (i % 2) * 0.03, 0.16 - i * 0.035);
                b.solid(ngon(h[0], h[1], 6, 0.024, i * 31), z - 0.03, 0.035, rock);
            }
            b.around(heap);
        }
        if (tier == 3) {
            int mark = b.mark();
            double[] f = turned(-0.17, -0.13);
            b.solid(circ(f[0], f[1], 0.012), z - 0.03, 0.55, iron);
            b.flat(poly(f[0], f[1] - 0.03, f[0] + 0.12, f[1] - 0.015, f[0], f[1]), z + 0.49, burst);
            b.around(mark);
        }
        b.top = Math.max(frameTop + 0.05, endZ + 0.04);
        return b;
    }

    // ---- Stormcaller: a tesla coil ----------------------------------------------------------------------------------

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
        double plate = tier == 1 ? r * 0.86 : r;
        b.solid(ngon(0, 0, 8, plate, 22.5), z, 0.05, iron);
        z += 0.05;
        for (int i = 0; i < 8; i++) {
            b.flat(at(circ(0, 0, 0.013), i * 45, plate * 0.84), z, iron.highlight());
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
        int column = b.mark();
        for (int i = 0; i < turns; i++) {
            double t = (double) i / (turns - 1);
            b.solid(circ(0, 0, 0.064 - 0.016 * t), z, 0.05, i % 2 == 0 ? copper : copperDark);
            z += 0.05;
        }
        if (tier >= 2) {
            int mark = b.mark();
            b.solid(ring(0, 0, wide + 0.025, wide - 0.012), foot + (z - foot) * 0.30, 0.03, blue);
            b.aroundColumn(column, mark);
        }
        if (tier >= 3) {
            int mark = b.mark();
            b.solid(ring(0, 0, wide - 0.02, wide - 0.05), foot + (z - foot) * 0.66, 0.03, blue);
            b.aroundColumn(column, mark);
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

    // ---- Hexer: an oracle's ball ------------------------------------------------------------------------------------

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
        if (tier == 3) {
            b.line(circ(0, 0, r * 0.94), z, alpha(hex, 0.9), 0.014);
            for (int i = 0; i < 6; i++) {
                b.line(open(cos(i * 60) * r * 0.86, sin(i * 60) * r * 0.86, cos(i * 60) * r, sin(i * 60) * r), z,
                        alpha(hex, 0.9), 0.014);
            }
        }
        int marks = b.mark();
        double standing = tier >= 2 ? 0.16 : 0.10;
        for (int i = 0; i < 3; i++) {
            b.solid(rot(box(0.16, 0, 0.17, 0.034), 90 + i * 120), z, 0.05, iron);
            b.solid(at(ngon(0, 0, 4, tier >= 2 ? 0.052 : 0.044, 20), 30 + i * 120, r * 0.74), z, standing, stone(hex));
            b.flat(at(box(0, 0, 0.012, 0.045), 30 + i * 120, r * 0.74), z + standing,
                    tier >= 2 ? lit : mix(lit, stone(hex).body(), 0.5));
        }
        b.around(marks);
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

    // ---- Mortar: a stationary siege mortar --------------------------------------------------------------------------

    /** The height of the floor the mortar and the rack stand on for {@code tier}: the plinth's top, then the deck. */
    static double mortarFloor(int tier) {
        return tier * STEP_HEIGHT + (tier >= 1 ? MORTAR_DECK : 0);
    }

    /** The muzzle's centre at the top of the barrel, in cells: x, y on the plan and z, for {@code tier} in {@code pose}. */
    static double[] mortarMouth(int tier, TowerPose pose) {
        int slices = MORTAR_SLICES + (pose.has(TowerCue.LONG_BARREL) ? 1 : 0);
        double rise = 0.04 + slices * MORTAR_SLICE - pose.recoil() * MORTAR_RECOIL;
        return new double[] {MORTAR_X, MORTAR_Y - MORTAR_LEAN * rise, mortarFloor(tier) + rise};
    }

    /**
     * The centre in cells (x, y, z) of rack place {@code i} of {@code count}: the bottom row centred, the rest lying in
     * its gaps on top.
     */
    static double[] mortarPiece(int tier, int i, int count) {
        double base = mortarFloor(tier) + MORTAR_RACK_HEIGHT + 0.02 + MORTAR_SHELL * 0.85;
        if (i < MORTAR_RACK_ROW) {
            int row = Math.min(count, MORTAR_RACK_ROW);
            return new double[] {(i - (row - 1) / 2.0) * MORTAR_RACK_GAP, MORTAR_RACK_Y, base};
        }
        int top = count - MORTAR_RACK_ROW;
        double x = top == 1 ? -MORTAR_RACK_GAP / 2 : (i - MORTAR_RACK_ROW - 0.5) * MORTAR_RACK_GAP;
        return new double[] {x, MORTAR_RACK_Y - MORTAR_SHELL * 0.3, base + MORTAR_SHELL * 1.4};
    }

    /** The rack place of {@code kind} among {@code stock} (the kinds in their order), the first if it is not there. */
    static int mortarPlace(List<LoadedShell> stock, LoadedShell kind) {
        return Math.max(0, stock.indexOf(kind));
    }

    /**
     * How many shells are in the barrel while {@code pose} reloads: a single load drops in at the reload's last
     * quarter, a volley one at each quarter from the shot.
     */
    static int mortarLoadedCount(TowerPose pose, boolean volley) {
        if (pose.flash()) {
            return 0;
        }
        if (volley) {
            return Math.min(3, (int) Math.floor(pose.reload() * 4));
        }
        return pose.reload() >= 0.75 ? 1 : 0;
    }

    /**
     * The kind whose top shows in the muzzle: the shell dropped in last. {@code load} is what this reload loads, front
     * (fired first) first; a volley loads it back to front, so the front shell goes in last and sits on top.
     */
    static LoadedShell mortarShowing(TowerPose pose, boolean volley, List<LoadedShell> load) {
        int in = mortarLoadedCount(pose, volley);
        return load.get(Math.max(0, load.size() - in));
    }

    /**
     * A stationary siege mortar standing in the middle of its cell, on a plank deck from Tier I: a bedplate, four
     * splayed legs braced across, a thick wide-mouthed barrel fixed at a steep angle to the north, and a ready rack in
     * front holding one shell of every kind the pose's stock names (what the tower can load, whatever its magazine
     * holds now). Nothing turns. A shot throws the barrel down its axis and flashes at the muzzle; while it reloads the
     * bore smokes, shells are lifted off the rack (their flight between is painted live) and the last one in shows its
     * top above the muzzle until the next shot. The rack never empties: it is the stock, not the magazine.
     */
    private static Build siegeMortar(int tier, TowerPose pose) {
        List<LoadedShell> stock = pose.stock().isEmpty() ? List.of(LoadedShell.NORMAL) : pose.stock();
        List<LoadedShell> load = pose.load().isEmpty() ? List.of(LoadedShell.NORMAL) : pose.load();
        Color mortar = colour(TowerLook.MORTAR);
        Tones iron = tones(0x5a6070);
        Tones dark = tones(0x3a3f4c);
        Tones bronze = Tones.of(mix(mortar, Color.WHITE, 0.12));
        Tones wood = tones(0x7a5a3a);
        Tones plank = Tones.of(mix(wood.shade(), new Color(0x2a2622), 0.4));
        boolean volley = pose.has(TowerCue.VOLLEY);
        Build b = plinth(tier, mortar, r -> ngon(0, 0, 8, r, 22.5));
        double r = b.bodyRadius;
        double z = b.top;
        if (tier >= 1) {
            double deck = r * 0.92;
            b.solid(ngon(0, 0, 8, deck, 22.5), z, MORTAR_DECK, plank);
            for (int i = -2; i <= 2; i++) {
                b.flat(box(i * deck * 0.36, 0, 0.006, deck * 1.7), z + MORTAR_DECK, plank.shade());
            }
            if (tier >= 2) {
                b.flat(ring(0, 0, deck, deck - 0.022), z + MORTAR_DECK, iron.body());
                for (int i = 0; i < 8; i++) {
                    b.flat(at(circ(0, 0, 0.012), 22.5 + i * 45, deck - 0.011), z + MORTAR_DECK, iron.highlight());
                }
            }
            z += MORTAR_DECK;
        }
        if (tier == 3) {
            int mark = b.mark();
            for (int i = 0; i < 5; i++) {
                double a = -180 + i * 45;
                b.solid(at(circ(0, 0, 0.018), a, r * 0.80), z, 0.14, iron);
                b.flat(at(circ(0, 0, 0.022), a, r * 0.80), z + 0.14, iron.highlight());
            }
            b.line(arcOver(r * 0.80, -180, 0, z + 0.12), 0, iron.shade(), 0.016);
            b.around(mark);
        }
        double legs = 0.19 + 0.012 * tier;
        double collar = z + 0.04 + MORTAR_SLICE * 2.5;
        double collarY = MORTAR_Y - MORTAR_LEAN * (collar - z);
        double[] feet = {-135, -45, 45, 135};
        for (double a : feet) {
            if (sin(a) < 0) {
                leg(b, MORTAR_X + cos(a) * legs, MORTAR_Y + sin(a) * legs, z, MORTAR_X + cos(a) * 0.10,
                        collarY + sin(a) * 0.08, collar, dark, tier);
            }
        }
        b.solid(ngon(MORTAR_X, MORTAR_Y, 8, 0.14 + 0.008 * tier, 22.5), z, 0.04, dark);
        b.flat(ring(MORTAR_X, MORTAR_Y, 0.125 + 0.008 * tier, 0.11 + 0.008 * tier), z + 0.04, iron.highlight());
        int slices = MORTAR_SLICES + (pose.has(TowerCue.LONG_BARREL) ? 1 : 0);
        double back = pose.recoil() * MORTAR_RECOIL;
        double rise = z + 0.04 - back;
        for (int k = 0; k < slices; k++) {
            double y = MORTAR_Y - MORTAR_LEAN * (rise - z);
            boolean band = k == 0 || k == 2 || k == slices - 1;
            double radius = k == 0 ? 0.122 : k == slices - 1 ? 0.128 : 0.110;
            b.solid(circ(MORTAR_X, y, radius), rise, MORTAR_SLICE, band ? bronze : iron);
            if (k == 2) {
                for (int s = -1; s <= 1; s += 2) {
                    b.solid(box(MORTAR_X + s * 0.128, y, 0.036, 0.046), rise + MORTAR_SLICE * 0.3, 0.032, dark);
                }
            }
            rise += MORTAR_SLICE;
        }
        double my = MORTAR_Y - MORTAR_LEAN * (rise - z);
        Shape lip = ring(MORTAR_X, my, MORTAR_MUZZLE, MORTAR_BORE);
        Area farLip = new Area(lip);
        farLip.intersect(new Area(new Rectangle2D.Double(-2, -2, 4, 2 + my)));
        Area nearLip = new Area(lip);
        nearLip.subtract(farLip);
        b.solid(farLip, rise, 0.03, bronze);
        b.flat(circ(MORTAR_X, my, MORTAR_BORE), rise + 0.03, new Color(0x120f0c));
        boolean loaded = mortarLoadedCount(pose, volley) > 0;
        if (loaded) {
            shell(b, mortarShowing(pose, volley, load), MORTAR_X, my, MORTAR_SHELL, rise + MORTAR_SHELL * 0.7);
        }
        b.solid(nearLip, rise, 0.03, bronze);
        if (pose.flash()) {
            b.glow(MORTAR_X, my, 0.24, rise + 0.07, alpha(new Color(0xffe6b0), 0.95));
            b.glow(MORTAR_X, my, 0.11, rise + 0.07, alpha(Color.WHITE, 0.9));
        } else if (pose.reload() < 0.25 && !loaded) {
            b.glow(MORTAR_X, my, 0.10, rise + 0.08, alpha(new Color(0xb9bcc8), 0.55));
            b.glow(MORTAR_X - 0.025, my - 0.04, 0.07, rise + 0.17, alpha(new Color(0xb9bcc8), 0.35));
        }
        if (loaded && pose.reload() >= 1) {
            b.light(Life.GLINT, MORTAR_X - 0.02, my - 0.03, rise + MORTAR_SHELL * 0.7, 0.05, Color.WHITE);
        }
        for (double a : feet) {
            if (sin(a) > 0) {
                leg(b, MORTAR_X + cos(a) * legs, MORTAR_Y + sin(a) * legs, z, MORTAR_X + cos(a) * 0.10,
                        collarY + sin(a) * 0.08, collar, dark, tier);
            }
        }
        double braceZ = z + (collar - z) * 0.45;
        double bx = legs * 0.62;
        double braceY = MORTAR_Y + bx * 0.75;
        b.line(open(MORTAR_X - bx * 0.75, braceY - LIFT * braceZ, MORTAR_X + bx * 0.75, braceY - LIFT * braceZ), 0,
                dark.shade(), 0.024);
        b.line(open(MORTAR_X - bx * 0.75, braceY - LIFT * braceZ, MORTAR_X + bx * 0.75, braceY - LIFT * braceZ), 0,
                dark.highlight(), 0.010);
        if (tier >= 2) {
            b.line(open(MORTAR_X - bx * 0.75, braceY - LIFT * z, MORTAR_X + bx * 0.75, braceY - LIFT * braceZ), 0,
                    dark.shade(), 0.018);
            b.line(open(MORTAR_X + bx * 0.75, braceY - LIFT * z, MORTAR_X - bx * 0.75, braceY - LIFT * braceZ), 0,
                    dark.shade(), 0.018);
        }
        int count = stock.size();
        int row = Math.min(count, MORTAR_RACK_ROW);
        double rackWide = row * MORTAR_RACK_GAP + 0.03;
        b.solid(box(0, MORTAR_RACK_Y, rackWide, MORTAR_RACK_GAP + 0.02), z, MORTAR_RACK_HEIGHT, wood);
        for (int s = -1; s <= 1; s += 2) {
            b.solid(box(s * (rackWide / 2 - 0.008), MORTAR_RACK_Y, 0.016, MORTAR_RACK_GAP + 0.02),
                    z + MORTAR_RACK_HEIGHT, 0.03, Tones.of(wood.shade()));
        }
        for (int i = 0; i < count; i++) {
            double[] p = mortarPiece(tier, i, count);
            rackedShell(b, stock.get(i), p[0], p[1], MORTAR_SHELL, p[2]);
        }
        b.top = rise + 0.03;
        return b;
    }

    /** A rail along the far half of a circle of radius {@code r}, from {@code from} to {@code to} degrees, at {@code z}. */
    private static Shape arcOver(double r, double from, double to, double z) {
        int steps = 24;
        double[] xy = new double[(steps + 1) * 2];
        for (int i = 0; i <= steps; i++) {
            double a = from + (to - from) * i / steps;
            xy[i * 2] = cos(a) * r;
            xy[i * 2 + 1] = sin(a) * r - LIFT * z;
        }
        return open(xy);
    }

    /**
     * One splayed leg from its foot on the floor to the collar round the barrel, a shaded line drawn in screen
     * measures (a line part at height 0), with a foot plate.
     */
    private static void leg(Build b, double fx, double fy, double fz, double cx, double cy, double cz, Tones t,
                            int tier) {
        double w = 0.034 + 0.004 * tier;
        b.solid(circ(fx, fy, w * 0.9), fz, 0.02, t);
        b.line(open(fx, fy - LIFT * fz, cx, cy - LIFT * cz), 0, t.shade(), w);
        b.line(open(fx - w * 0.2, fy - LIFT * fz - w * 0.2, cx - w * 0.2, cy - LIFT * cz - w * 0.2), 0,
                t.highlight(), w * 0.35);
    }

    /** A shell of {@code kind} on a rack, without its glow: there a glow would hide its neighbours. */
    private static void rackedShell(Build h, LoadedShell kind, double x, double y, double r, double z) {
        for (Part p : shell(kind, r)) {
            if (p.kind() != Kind.GLOW) {
                h.parts.add(new Part(p.kind(), mv(p.shape(), x, y), p.z0() + z, p.height(), p.tones(), p.color(),
                        p.width()));
            }
        }
    }

    /** A shell of {@code kind} on its own, {@code r} across, lying at the origin. */
    static List<Part> shell(LoadedShell kind, double r) {
        Build b = new Build();
        shell(b, kind, 0, 0, r, 0);
        return List.copyOf(b.parts);
    }

    /** One shell kind: the same drawing in the barrel, on the tray and on its own. */
    private static void shell(Build b, LoadedShell kind, double x, double y, double r, double z) {
        switch (kind) {
            case NORMAL -> {
                b.solid(circ(x, y, r), z, 0.02, tones(0x7b8496));
                b.flat(circ(x - r * 0.3, y - r * 0.3, r * 0.3), z + 0.02, new Color(0xc4cad6));
            }
            case HEAVY -> {
                b.solid(circ(x, y, r * 1.25), z, 0.02, tones(0x7b8496));
                b.flat(box(x, y, r * 2.3, r * 0.5), z + 0.02, new Color(0xf2d28a));
            }
            case NUKE -> {
                b.glow(x, y, r * 3.2, z, alpha(new Color(0xc8f04a), 0.7));
                b.solid(circ(x, y, r * 1.5), z, 0.02, tones(0xe8e8dc));
                b.flat(ngon(x, y, 3, r * 0.9, -90), z + 0.02, new Color(0x2a2a20));
            }
            case NAPALM -> {
                b.solid(circ(x, y, r), z, 0.02, tones(0xff7a2a));
                b.flat(circ(x - r * 0.2, y - r * 0.2, r * 0.4), z + 0.02, new Color(0xffd34d));
            }
            case TAR -> {
                b.solid(circ(x, y, r), z, 0.02, new Tones(new Color(0x4b3c5a), new Color(0x241d2a), new Color(0x6a5a80)));
                b.flat(circ(x - r * 0.3, y - r * 0.35, r * 0.28), z + 0.02, new Color(0xf0eaff));
            }
            case CRYO -> {
                b.solid(circ(x, y, r), z, 0.02, tones(0xaae6ff));
                b.flat(star(x, y, 4, r * 0.7, r * 0.2, 0), z + 0.02, Color.WHITE);
            }
            case FLARE -> {
                b.glow(x, y, r * 2.4, z, alpha(new Color(0xfff2a8), 0.8));
                b.flat(star(x, y, 4, r * 1.2, r * 0.32, 45), z + 0.02, new Color(0xfffbe0));
            }
        }
    }

    // ---- Scorcher ----------------------------------------------------------------------------------------------------

    /**
     * A fuel tank, then a tank and its reserve, then a grated firebox, then an armoured furnace whose vents glow while
     * it pours. {@code soul} is the dark metal that burns violet.
     */
    private static Build scorcher(int tier, TowerPose pose, boolean soul) {
        Tones metal = soul ? new Tones(new Color(0x1f1b2a), new Color(0x3d3650), new Color(0x8a78c0))
                : Tones.of(colour(TowerLook.CINDER));
        Tones iron = tones(soul ? 0x3a3648 : 0x565a66);
        Color hot = soul ? SOUL_FLAME : new Color(0xffb347);
        Build b = plinth(tier, soul ? SOUL_STONE : colour(TowerLook.CINDER), r -> ngon(0, 0, 16, r, 0));
        double r = b.bodyRadius;
        double z = b.top;
        boolean streaming = pose.has(TowerCue.STREAMING);
        if (tier == 0) {
            b.solid(circ(0, 0, r * 0.74), z, 0.16, metal);
            b.line(circ(0, 0, r * 0.5), z + 0.16, metal.shade(), 0.016);
            z += 0.16;
        } else if (tier == 1) {
            b.solid(circ(-0.15, 0.13, 0.085), z, 0.12, metal);
            b.line(circ(-0.15, 0.13, 0.05), z + 0.12, metal.shade(), 0.014);
            b.solid(circ(0.02, -0.02, r * 0.62), z, 0.16, metal);
            b.line(circ(0.02, -0.02, r * 0.42), z + 0.16, metal.shade(), 0.016);
            b.line(open(-0.15, 0.13, -0.16, 0.0, -0.06, -0.04), z + 0.16, new Color(0x2c2c32), 0.03);
            z += 0.16;
        } else if (tier == 2) {
            double g = r * 0.80;
            b.flat(box(0, 0, g * 2, g * 2), z, soul ? new Color(0x2a1c4a) : new Color(0x5a1e10));
            for (int i = 0; i < 5; i++) {
                b.flat(circ(-g * 0.7 + i * g * 0.35, (i % 2 == 0 ? -1 : 1) * g * 0.72, 0.022), z, hot);
            }
            b.solid(minus(box(0, 0, g * 2.1, g * 2.1), box(0, 0, g * 1.74, g * 1.74)), z, 0.07, iron);
            for (int i = -1; i <= 1; i++) {
                b.line(open(i * g * 0.5, -g * 0.9, i * g * 0.5, g * 0.9), z + 0.07, iron.body(), 0.022);
            }
            b.solid(circ(-g * 0.62, g * 0.62, 0.075), z + 0.07, 0.11, metal);
            b.solid(circ(g * 0.62, g * 0.62, 0.075), z + 0.07, 0.11, metal);
            b.solid(circ(0, -0.02, 0.135), z + 0.07, 0.15, metal);
            b.line(circ(0, -0.02, 0.09), z + 0.22, metal.shade(), 0.016);
            z += 0.22;
        } else {
            b.solid(ngon(0, 0, 8, r, 22.5), z, 0.22, iron);
            z += 0.22;
            b.line(ngon(0, 0, 8, r * 0.86, 22.5), z, metal.body(), 0.028);
            Color lit = soul ? new Color(0xd9c8ff) : new Color(0xffd34d);
            Color cold = soul ? new Color(0x4a3a80) : new Color(0x8a3418);
            for (int i = 0; i < 4; i++) {
                b.flat(at(box(0, 0, 0.045, 0.12), 45 + i * 90, r * 0.60), z, streaming ? lit : cold);
                if (streaming) {
                    b.glow(cos(45 + i * 90) * r * 0.60, sin(45 + i * 90) * r * 0.60, 0.09, z, alpha(hot, 0.7));
                }
            }
            b.solid(circ(-0.105, 0.12, 0.08), z, 0.10, metal);
            b.solid(circ(0.105, 0.12, 0.08), z, 0.10, metal);
            b.solid(circ(0, 0, 0.11), z, 0.05, metal);
            z += 0.05;
        }
        b.top = z;
        if (soul) {
            return b.aim(soulHead(z, pose));
        }
        if (pose.has(TowerCue.DRAGONS_BREATH)) {
            return b.aim(dragonHead(z, pose));
        }
        return b.aim(nozzle(z, pose, metal, iron));
    }

    /** The plain nozzle: a pipe to a flared tip that cools as it reloads, a pilot flame while it waits. */
    private static Build nozzle(double z, TowerPose pose, Tones metal, Tones iron) {
        boolean streaming = pose.has(TowerCue.STREAMING);
        double len = pose.has(TowerCue.LONG_BARREL) ? 0.31 : 0.25;
        Color pilot = new Color(0xffd34d);
        Color tip = streaming ? new Color(0xfff2c0) : mix(new Color(0xff5a2a), iron.body(), pose.reload());
        Build h = new Build();
        h.solid(circ(0, 0, 0.066), z, 0.03, iron);
        h.solid(box(len / 2, 0, len, 0.05), z + 0.03, 0.03, Tones.of(metal.highlight()));
        h.solid(poly(len - 0.01, -0.028, len + 0.065, -0.055, len + 0.065, 0.055, len - 0.01, 0.028), z + 0.03, 0.035,
                Tones.of(tip));
        if (!streaming) {
            h.flat(poly(len + 0.065, -0.014, len + 0.105, 0, len + 0.065, 0.014), z + 0.065, pilot);
            h.light(Life.FLICKER, len + 0.085, 0, z + 0.065, 0.06, pilot);
        }
        return h;
    }

    // ---- Scorcher: the dragon's head --------------------------------------------------------------------------------

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

    // ---- Scorcher: Soulfire's head ----------------------------------------------------------------------------------

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

    // ---- Aerie ------------------------------------------------------------------------------------------------------

    /**
     * The plain Aerie: a crystal spire that narrows as it climbs, the tallest thing on the board; nothing rings it. Its
     * tip turns white and glows while a Release is ready, as Ruby's, Shade's and White's do.
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
        b.flat(circ(0, 0, r * AERIE_FOOT_SHARE), z, AERIE_FOOT);
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
        for (int i = 0; i < share.length; i++) {
            Shape plan = tier == 3 && i == 0 ? star(0, 0, 6, 0.15, 0.095, 0) : ngon(0, 0, 6, wide * taper[i], 0);
            b.solid(plan, z, height * share[i], i == 3 ? pale : crystal);
            z += height * share[i];
        }
        b.flat(ngon(0, 0, 6, wide * 0.2, 30), z, release ? Color.WHITE : mix(lit, Color.WHITE, 0.55));
        if (release) {
            b.glow(0, 0, 0.26, z, alpha(lit, 0.8));
        }
        b.light(Life.BREATH, 0, 0, z, 0.15, lit);
        b.top = z;
        b.nest = z;
        return b;
    }

    // ---- Aerie: Ruby ------------------------------------------------------------------------------------------------

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
        b.nest = z;
        return b;
    }

    // ---- Aerie: Shade -----------------------------------------------------------------------------------------------

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
        b.nest = b.top;
        return b;
    }

    // ---- Aerie: the White tower -------------------------------------------------------------------------------------

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
        b.nest = z;
        return b;
    }

    // ---- Obelisk: a menhir ------------------------------------------------------------------------------------------

    /** The menhir's plan at its foot: a rough slab, its south edge straight along the rune face. */
    private static Shape menhirPlan() {
        return poly(-0.110, MENHIR_FACE, -0.125, -0.010, -0.075, -0.080, 0.025, -0.095, 0.100, -0.055, 0.125, 0.020,
                0.105, MENHIR_FACE);
    }

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

    // ---- Beacon ------------------------------------------------------------------------------------------------------

    /** A post with a lantern, rising to a lighthouse with eight panes; a pane takes the colour of a look it is lit by. */
    private static Build beacon(int tier, TowerPose pose) {
        Color beacon = colour(TowerLook.AURA);
        Tones gold = Tones.of(beacon);
        Tones stone = stone(beacon);
        boolean day = pose.has(TowerCue.DAYLIGHT);
        Color light = day ? DAYLIGHT : LANTERN_LIGHT;
        Build b = plinth(tier, beacon, r -> ngon(0, 0, 8, r, 22.5));
        double r = b.bodyRadius;
        double z = b.top;
        double post = tier >= 2 ? r * 0.8 : 0.14;
        double height = 0.22 + 0.13 * tier;
        b.solid(circ(0, 0, post), z, height, tier >= 2 ? Tones.of(stone.highlight()) : stone);
        z += height;
        if (tier == 3) {
            b.solid(circ(0, 0, post * 1.12), z, 0.03, stone);
            z += 0.03;
            for (int i = 0; i < 8; i++) {
                b.flat(at(circ(0, 0, 0.014), i * 45, post), z, gold.highlight());
            }
        }
        b.glow(0, 0, tier == 0 ? 0.25 : 0.30, z, alpha(light, day ? 0.75 : 0.5));
        if (tier == 0) {
            b.solid(ngon(0, 0, 4, 0.105, 0), z, 0.08, gold);
            b.flat(ngon(0, 0, 4, 0.06, 0), z + 0.08, light);
            b.light(Life.BREATH, 0, 0, z + 0.08, 0.2, light);
        } else {
            double pane = tier == 1 ? 0.125 : 0.14;
            b.solid(ngon(0, 0, 8, pane + 0.012, 22.5), z, 0.06, gold);
            z += 0.06;
            for (int i = 0; i < 8; i++) {
                double a0 = 22.5 + i * 45;
                double a1 = a0 + 45;
                Color glass = pose.panes().isEmpty() ? (i % 2 == 0 ? light : mix(light, beacon, 0.35))
                        : colour(pose.panes().get(i % pose.panes().size()));
                b.flat(poly(cos(a0) * pane, sin(a0) * pane, cos(a1) * pane, sin(a1) * pane,
                        cos(a1) * 0.05, sin(a1) * 0.05, cos(a0) * 0.05, sin(a0) * 0.05), z, glass);
                b.line(open(cos(a0) * pane, sin(a0) * pane, cos(a0) * 0.05, sin(a0) * 0.05), z, gold.shade(), 0.01);
            }
            b.solid(ngon(0, 0, 4, 0.075, 0), z, 0.05, gold);
            if (tier == 3) {
                b.flat(circ(0, 0, 0.018), z + 0.05, Color.WHITE);
            }
            b.light(Life.SWEEP, 0, 0, z, pane * 3.4, light);
        }
        b.top = z;
        return b;
    }
}
