package td.ui;

import td.ui.render.TowerLook;
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
import java.util.Optional;
import java.util.Set;
import java.util.function.DoubleFunction;
import java.util.function.IntFunction;

/**
 * The table of tower foundations: one row per {@link TowerLook}, checked complete when this class loads. A foundation is
 * a stack of flat parts in cell units (the cell's centre at 0,0, a cell 1 wide, north up), each with a height; it never
 * moves, and it grows a step with every tier. A look that aims also has a head, built at the foundation's top.
 */
final class TowerFoundations {

    /** The tiers a foundation is drawn at; a tier above the last wears the last's. */
    static final int TIERS = 4;

    private static final double[] FOOTPRINT = {0.275, 0.315, 0.36, 0.40};
    private static final double PLINTH_STEP = 0.045;
    private static final double STEP_HEIGHT = 0.07;
    private static final Color PLINTH_STONE = new Color(0x50535e);
    private static final Color PAVING_DARK = new Color(0x101014);
    private static final Color GUNMETAL = new Color(0x59626a);
    private static final Color SHELL_IRON = new Color(0x7b8496);
    private static final Color SHELL_GLINT = new Color(0xc4cad6);

    private static final Map<TowerLook, Row> ROWS = checkedComplete(rows());
    private static final Map<TowerLook, List<Foundation>> BUILT = built();

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

        /** The same part standing {@code dz} higher. */
        Part raisedBy(double dz) {
            return new Part(this.kind, this.shape, this.z0 + dz, this.height, this.tones, this.color, this.width);
        }
    }

    /**
     * A foundation's parts in paint order, back to front; {@code top} is where its head stands and {@code footprint}
     * its radius on the ground.
     */
    record Foundation(List<Part> parts, double top, double footprint) {

        Foundation {
            parts = List.copyOf(parts);
        }
    }

    /** A look's foundation at each tier, and the head it turns, if it aims. */
    private record Row(IntFunction<Build> foundation, Optional<List<Part>> head) {
    }

    /** The foundation {@code look} stands on at {@code tier}. */
    static Foundation of(TowerLook look, int tier) {
        return BUILT.get(look).get(Math.clamp(tier, 0, TIERS - 1));
    }

    /** The head {@code look} aims, its parts' heights counted from the foundation's top, pointing along {@code +X}. */
    static Optional<List<Part>> head(TowerLook look) {
        return ROWS.get(look).head();
    }

    static <V> Map<TowerLook, V> checkedComplete(Map<TowerLook, V> rows) {
        Set<TowerLook> missing = EnumSet.allOf(TowerLook.class);
        missing.removeAll(rows.keySet());
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Tower looks without a foundation: " + missing);
        }
        return rows;
    }

    private static Map<TowerLook, List<Foundation>> built() {
        Map<TowerLook, List<Foundation>> built = new EnumMap<>(TowerLook.class);
        ROWS.forEach((look, row) -> {
            List<Foundation> tiers = new ArrayList<>(TIERS);
            for (int tier = 0; tier < TIERS; tier++) {
                Build build = row.foundation().apply(tier).finish();
                tiers.add(new Foundation(build.parts, build.top, build.footprint));
            }
            built.put(look, List.copyOf(tiers));
        });
        return built;
    }

    private static Map<TowerLook, Row> rows() {
        Map<TowerLook, Row> rows = new EnumMap<>(TowerLook.class);
        rows.put(TowerLook.SNIPER, new Row(TowerFoundations::sniper, Optional.of(sniperHead())));
        rows.put(TowerLook.SPLASH, new Row(tier -> tier == 0 ? thrower() : stormcaller(tier), Optional.empty()));
        rows.put(TowerLook.STORMCALLER, new Row(tier -> stormcaller(Math.max(1, tier)), Optional.empty()));
        rows.put(TowerLook.HEXER, new Row(tier -> hexer(Math.max(1, tier)), Optional.empty()));
        rows.put(TowerLook.MORTAR, new Row(TowerFoundations::mortar, Optional.of(mortarHead())));
        rows.put(TowerLook.CINDER, new Row(TowerFoundations::scorcher, Optional.of(scorcherHead())));
        rows.put(TowerLook.SEEKER, new Row(TowerFoundations::aerie, Optional.empty()));
        rows.put(TowerLook.PULSE, new Row(TowerFoundations::obelisk, Optional.empty()));
        rows.put(TowerLook.AURA, new Row(TowerFoundations::beacon, Optional.empty()));
        return rows;
    }

    // ---- colour ----------------------------------------------------------------------------------------------------

    private static Color mix(Color a, Color b, double share) {
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

    private static Shape circ(double x, double y, double r) {
        return new Ellipse2D.Double(x - r, y - r, r * 2, r * 2);
    }

    private static Shape ell(double x, double y, double rx, double ry) {
        return new Ellipse2D.Double(x - rx, y - ry, rx * 2, ry * 2);
    }

    private static Shape box(double x, double y, double w, double h) {
        return new Rectangle2D.Double(x - w / 2, y - h / 2, w, h);
    }

    /** A regular polygon, its first corner at {@code deg} (0 is east, -90 north). */
    private static Shape ngon(double x, double y, int n, double r, double deg) {
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

    private static Shape poly(double... xy) {
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

    private static Shape minus(Shape a, Shape b) {
        Area area = new Area(a);
        area.subtract(new Area(b));
        return area;
    }

    private static Shape rot(Shape s, double deg) {
        return AffineTransform.getRotateInstance(Math.toRadians(deg)).createTransformedShape(s);
    }

    private static Shape mv(Shape s, double dx, double dy) {
        return AffineTransform.getTranslateInstance(dx, dy).createTransformedShape(s);
    }

    /** {@code s} turned {@code deg} and moved {@code r} out along that bearing. */
    private static Shape at(Shape s, double deg, double r) {
        double a = Math.toRadians(deg);
        return mv(rot(s, deg), Math.cos(a) * r, Math.sin(a) * r);
    }

    private static double cos(double deg) {
        return Math.cos(Math.toRadians(deg));
    }

    private static double sin(double deg) {
        return Math.sin(Math.toRadians(deg));
    }

    // ---- a foundation under construction ---------------------------------------------------------------------------

    /** Parts from the ground up, in paint order. */
    @ThreadConfined(ThreadConfined.Owner.ENCLOSING)
    private static final class Build {

        private final List<Part> parts = new ArrayList<>();
        /** What stands south of the tower's centre: painted last, over everything behind it. */
        private final List<Part> front = new ArrayList<>();
        private double top;
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

    // ---- Sniper: a watchtower of triangles ---------------------------------------------------------------------------

    private static Build sniper(int tier) {
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
        return b;
    }

    /** The rifle on its swivel, a scope over it. */
    private static List<Part> sniperHead() {
        Color green = colour(TowerLook.SNIPER);
        Tones steel = Tones.of(mix(green, Color.WHITE, 0.55));
        Tones gunmetal = Tones.of(GUNMETAL);
        double len = 0.30;
        Build h = new Build();
        h.solid(circ(0, 0, 0.058), 0, 0.03, gunmetal);
        h.solid(box((0.04 + len) / 2, 0, len - 0.04, 0.034), 0.03, 0.03, steel);
        h.solid(box(-0.015, 0, 0.13, 0.072), 0.03, 0.04, Tones.of(green));
        h.solid(box(0.035, -0.05, 0.10, 0.028), 0.05, 0.03, gunmetal);
        h.flat(circ(0.09, -0.05, 0.017), 0.08, mix(new Color(0x2c4030), new Color(0xf2fff0), 0.3));
        return List.copyOf(h.parts);
    }

    // ---- Burst: the stone thrower, and its two forks -----------------------------------------------------------------

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

    /** A ring of stones round a throwing arm, cocked back with a stone in its cup. */
    private static Build thrower() {
        Color burst = colour(TowerLook.SPLASH);
        Tones wood = Tones.of(burst);
        Build b = plinth(0, burst, r -> circ(0, 0, r));
        double r = b.bodyRadius;
        b.flat(circ(0, 0, r * 0.74), 0, new Color(0x2b2422));
        stoneRing(b, r * 0.78, 0, burst, true);
        b.solid(box(0, -0.075, 0.05, 0.045), 0, 0.14, wood);
        b.solid(box(0, 0.075, 0.05, 0.045), 0, 0.14, wood);
        b.solid(box(0, 0, 0.026, 0.17), 0.12, 0.02, tones(0x6a5a52));
        b.solid(rot(box(0.085, 0, 0.17, 0.034), 140), 0.14, 0.03, wood);
        b.solid(at(circ(0, 0, 0.046), 140, 0.17), 0.14, 0.03, Tones.of(wood.shade()));
        b.solid(at(ngon(0, 0, 6, 0.032, 10), 140, 0.17), 0.17, 0.03, tones(0xd8c8c0));
        b.top = 0.2;
        return b;
    }

    /** A copper coil, taller with every tier, a ball at its top. */
    private static Build stormcaller(int tier) {
        Color storm = colour(TowerLook.STORMCALLER);
        Tones copper = tones(0xc98a5c);
        Tones iron = tones(0x596074);
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
                b.solid(at(circ(0, 0, 0.042), 30 + i * 60, r * 0.74), z, 0.14, tones(0xdfe6f5));
                b.flat(at(circ(0, 0, 0.018), 30 + i * 60, r * 0.74), z + 0.14, blue.body());
            }
            b.around(mark);
        }
        int turns = 2 + tier;
        double wide = 0.105 + 0.012 * tier;
        for (int i = 0; i < turns; i++) {
            double t = (double) i / (turns - 1);
            b.solid(circ(0, 0, wide - (wide - 0.06) * t), z, 0.085, copper);
            z += 0.085;
        }
        if (tier >= 2) {
            int mark = b.mark();
            b.solid(ring(0, 0, wide + 0.025, wide - 0.012), z - 0.05, 0.03, blue);
            b.around(mark);
        }
        if (tier >= 3) {
            b.solid(ring(0, 0, wide - 0.03, wide - 0.06), z + 0.04, 0.03, blue);
            z += 0.06;
        }
        b.solid(circ(0, 0, 0.048), z, 0.05, blue);
        b.top = z;
        return b;
    }

    /** An iron cauldron of brew; standing stones and tusks come with the tiers. */
    private static Build hexer(int tier) {
        Color hex = colour(TowerLook.HEXER);
        Tones iron = tones(0x555062);
        Tones bone = tones(0xe6dcc4);
        Color brew = mix(hex, new Color(0x3a1c60), 0.35);
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
                b.solid(at(ngon(0, 0, 4, 0.052, 20), 30 + i * 120, r * 0.74), z, 0.12, stone(hex));
                b.flat(at(box(0, 0, 0.012, 0.045), 30 + i * 120, r * 0.74), z + 0.12, lit);
            }
            b.around(mark);
        }
        double pot = tier >= 2 ? 0.15 : 0.14;
        double lift = tier >= 2 ? 0.10 : 0;
        b.solid(circ(0, 0, pot), z + lift, 0.14, iron);
        z += lift + 0.14;
        b.flat(circ(0, 0, pot * 0.8), z, brew);
        b.flat(circ(-0.045, -0.02, 0.026), z, lit);
        b.flat(circ(0.04, 0.045, 0.018), z, lit);
        b.flat(circ(0.05, -0.05, 0.012), z, lit);
        if (tier == 3) {
            Shape tusk = minus(circ(0, 0, 0.105), circ(0.04, 0, 0.11));
            int mark = b.mark();
            for (int i = 0; i < 3; i++) {
                b.solid(rot(mv(tusk, -0.085, 0), 30 + i * 120), z + 0.05, 0.05, bone);
            }
            b.around(mark);
        }
        b.top = z;
        return b;
    }

    // ---- Mortar ------------------------------------------------------------------------------------------------------

    /** A squat drum on sandbags, then on an iron ring, then inside a crenellated wall with shells stacked. */
    private static Build mortar(int tier) {
        Color mortar = colour(TowerLook.MORTAR);
        Tones iron = tones(0x5a6070);
        Tones brown = Tones.of(mortar);
        Tones sand = tones(0xb5a27c);
        Build b = plinth(tier, mortar, r -> ngon(0, 0, 8, r, 22.5));
        double r = b.bodyRadius;
        double z = b.top;
        if (tier == 3) {
            int mark = b.mark();
            b.solid(minus(ngon(0, 0, 8, r * 1.02, 22.5), ngon(0, 0, 8, r * 0.78, 22.5)), z, 0.16, stone(mortar));
            for (int i = 0; i < 8; i++) {
                b.solid(at(box(0, 0, 0.05, 0.06), 22.5 + i * 45, r * 0.9), z + 0.16, 0.05,
                        Tones.of(stone(mortar).highlight()));
            }
            b.around(mark);
            b.flat(ngon(0, 0, 8, r * 0.78, 22.5), z, new Color(0x2a2622));
            mark = b.mark();
            for (int i = 0; i < 3; i++) {
                shell(b, cos(118 + i * 22) * r * 0.62, sin(118 + i * 22) * r * 0.62, 0.028, z);
            }
            b.around(mark);
        } else if (tier >= 1) {
            int mark = b.mark();
            for (int i = 0; i < 10; i++) {
                b.solid(at(ell(0, 0, 0.042, 0.066), i * 36, r * 0.84), z, 0.07, sand);
            }
            b.around(mark);
        }
        double base = tier == 0 ? r * 0.8 : r * 0.6;
        b.solid(circ(0, 0, base), z, 0.08, tier >= 2 ? iron : brown);
        z += 0.08;
        if (tier >= 2) {
            b.flat(ring(0, 0, base * 0.82, base * 0.66), z, brown.body());
            for (int i = 0; i < 8; i++) {
                b.flat(at(circ(0, 0, 0.011), i * 45, base * 0.9), z, iron.highlight());
            }
        } else {
            b.solid(circ(0, 0, base * 0.68), z, 0.02, iron);
        }
        b.top = z;
        return b;
    }

    /** The wide barrel on its trunnions, a shell loaded in its open bore. */
    private static List<Part> mortarHead() {
        Tones brown = Tones.of(mix(colour(TowerLook.MORTAR), Color.WHITE, 0.12));
        Tones iron = tones(0x6a7080);
        double z = 0.02;
        double len = 0.21;
        Build h = new Build();
        h.solid(box(0, 0, 0.055, 0.2), z, 0.05, iron);
        h.solid(box(len / 2 - 0.04, 0, len + 0.04, 0.135), z, 0.10, brown);
        h.solid(box(len - 0.015, 0, 0.04, 0.16), z, 0.11, Tones.of(brown.highlight()));
        h.flat(ell(len - 0.05, 0, 0.042, 0.05), z + 0.11, new Color(0x17130f));
        shell(h, len - 0.05, 0, 0.034, z + 0.11);
        return List.copyOf(h.parts);
    }

    /** A plain shell: a lit iron ball. */
    private static void shell(Build b, double x, double y, double r, double z) {
        b.solid(circ(x, y, r), z, 0.02, Tones.of(SHELL_IRON));
        b.flat(circ(x - r * 0.3, y - r * 0.3, r * 0.3), z + 0.02, SHELL_GLINT);
    }

    // ---- Scorcher ----------------------------------------------------------------------------------------------------

    /** A fuel tank, then a tank and its reserve, then a grated firebox, then an armoured furnace. */
    private static Build scorcher(int tier) {
        Color scorcher = colour(TowerLook.CINDER);
        Tones metal = Tones.of(scorcher);
        Tones iron = tones(0x565a66);
        Color hot = new Color(0xffb347);
        Build b = plinth(tier, scorcher, r -> ngon(0, 0, 16, r, 0));
        double r = b.bodyRadius;
        double z = b.top;
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
            b.flat(box(0, 0, g * 2, g * 2), z, new Color(0x5a1e10));
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
            for (int i = 0; i < 4; i++) {
                b.flat(at(box(0, 0, 0.045, 0.12), 45 + i * 90, r * 0.60), z, new Color(0x8a3418));
            }
            b.solid(circ(-0.105, 0.12, 0.08), z, 0.10, metal);
            b.solid(circ(0.105, 0.12, 0.08), z, 0.10, metal);
            b.solid(circ(0, 0, 0.11), z, 0.05, metal);
            z += 0.05;
        }
        b.top = z;
        return b;
    }

    /** The nozzle on its swivel, a pilot flame at its mouth. */
    private static List<Part> scorcherHead() {
        Tones metal = Tones.of(colour(TowerLook.CINDER));
        Tones iron = tones(0x565a66);
        double len = 0.25;
        Build h = new Build();
        h.solid(circ(0, 0, 0.066), 0, 0.03, iron);
        h.solid(box(len / 2, 0, len, 0.05), 0.03, 0.03, Tones.of(metal.highlight()));
        h.solid(poly(len - 0.01, -0.028, len + 0.065, -0.055, len + 0.065, 0.055, len - 0.01, 0.028), 0.03, 0.035,
                Tones.of(iron.body()));
        h.flat(poly(len + 0.065, -0.014, len + 0.105, 0, len + 0.065, 0.014), 0.065, new Color(0xffd34d));
        return List.copyOf(h.parts);
    }

    // ---- Aerie -------------------------------------------------------------------------------------------------------

    /** A crystal spire on a hexagonal perch, taller with every tier; a nest ring, shards and arms come with the tiers. */
    private static Build aerie(int tier) {
        Color tint = colour(TowerLook.SEEKER);
        Tones crystal = Tones.of(tint);
        Tones stone = stone(tint);
        Color lit = crystal.highlight();
        Build b = plinth(tier, tint, r -> ngon(0, 0, 12, r, 15));
        double r = b.bodyRadius;
        double z = b.top;
        b.solid(ngon(0, 0, 6, r * 0.78, 30), z, 0.08, Tones.of(mix(tint, new Color(0x10202c), 0.5)));
        z += 0.08;
        if (tier >= 2) {
            int shards = tier == 3 ? 6 : 4;
            int mark = b.mark();
            for (int i = 0; i < shards; i++) {
                double a = (tier == 3 ? 0 : 45) + i * 360.0 / shards;
                b.solid(at(poly(-0.05, 0, 0, -0.026, 0.06, 0, 0, 0.026), a, r * 0.52), z, 0.13, Tones.of(lit));
            }
            b.around(mark);
        }
        if (tier >= 1) {
            int mark = b.mark();
            b.solid(ring(0, 0, 0.185, 0.15), z + 0.2, 0.025, crystal);
            b.around(mark);
        }
        if (tier == 3) {
            int mark = b.mark();
            for (int i = 0; i < 3; i++) {
                b.solid(rot(box(0.17, 0, 0.16, 0.03), -90 + i * 120), z + 0.3, 0.025, stone);
                b.solid(at(ring(0, 0, 0.048, 0.026), -90 + i * 120, 0.25), z + 0.3, 0.03, crystal);
            }
            b.around(mark);
        }
        double height = 0.45 + 0.15 * tier;
        if (tier == 3) {
            b.solid(star(0, 0, 6, 0.14, 0.09, 0), z, height, crystal);
        } else {
            b.solid(ngon(0, 0, 6, 0.10 + 0.008 * tier, 0), z, height, crystal);
        }
        z += height;
        b.flat(ngon(0, 0, 6, 0.045, 30), z, mix(lit, Color.WHITE, 0.55));
        b.top = z;
        return b;
    }

    // ---- Obelisk -----------------------------------------------------------------------------------------------------

    /** A dark stone with a pyramidion and a glowing rune; standing stones ring it from Tier II. */
    private static Build obelisk(int tier) {
        Tones dark = new Tones(new Color(0x48446c), new Color(0x6a6490), new Color(0xa39ad0));
        Build b = plinth(tier, colour(TowerLook.PULSE), r -> box(0, 0, r * 1.72, r * 1.72));
        double z = b.top;
        double reach = (b.footprint - PLINTH_STEP * 0.5) * 0.86 - 0.035;
        if (tier == 2) {
            int mark = b.mark();
            for (int i = 0; i < 3; i++) {
                b.solid(at(box(0, 0, 0.07, 0.07), -90 + i * 120, reach * 0.9), STEP_HEIGHT, 0.2, dark);
            }
            b.around(mark);
        }
        if (tier == 3) {
            int mark = b.mark();
            for (int i = 0; i < 8; i++) {
                double a = 22.5 + i * 45;
                double d = reach / Math.max(Math.abs(cos(a)), Math.abs(sin(a))) * 0.93;
                b.solid(mv(box(0, 0, 0.066, 0.066), cos(a) * d, sin(a) * d), STEP_HEIGHT, 0.22, dark);
            }
            b.around(mark);
        }
        double side = tier == 3 ? 0.30 : 0.26;
        double height = 0.45 + 0.12 * tier;
        b.solid(box(0, 0, side, side), z, height, dark);
        z += height;
        b.solid(box(0, 0, side * 0.86, side * 0.86), z, 0.2, dark);
        z += 0.2;
        double half = side * 0.43;
        b.flat(poly(0, 0, -half, -half, half, -half), z, mix(dark.highlight(), Color.WHITE, 0.15));
        b.flat(poly(0, 0, -half, -half, -half, half), z, dark.highlight());
        b.flat(poly(0, 0, half, -half, half, half), z, dark.body());
        b.flat(poly(0, 0, -half, half, half, half), z, dark.shade());
        b.top = z;
        rune(b, z, TowerLooks.tierColor(TowerLook.PULSE, tier));
        return b;
    }

    /** The one glowing part of the stone, in its tier's colour: a cross. */
    private static void rune(Build b, double z, Color c) {
        double scale = 1.5;
        double w = 0.016 * scale;
        b.glow(0, 0, 0.15, z, alpha(c, 0.55 * 0.9));
        b.line(open(0, -0.045 * scale, 0, 0.045 * scale), z, c, w);
        b.line(open(-0.03 * scale, -0.012 * scale, 0.03 * scale, -0.012 * scale), z, c, w);
    }

    // ---- Beacon ------------------------------------------------------------------------------------------------------

    /** A post with a lantern, rising to a lighthouse with eight panes. */
    private static Build beacon(int tier) {
        Color beacon = colour(TowerLook.AURA);
        Tones gold = Tones.of(beacon);
        Tones stone = stone(beacon);
        Color light = new Color(0xffe9a8);
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
        b.glow(0, 0, tier == 0 ? 0.25 : 0.30, z, alpha(light, 0.5));
        if (tier == 0) {
            b.solid(ngon(0, 0, 4, 0.105, 0), z, 0.08, gold);
            b.flat(ngon(0, 0, 4, 0.06, 0), z + 0.08, light);
        } else {
            double pane = tier == 1 ? 0.125 : 0.14;
            b.solid(ngon(0, 0, 8, pane + 0.012, 22.5), z, 0.06, gold);
            z += 0.06;
            for (int i = 0; i < 8; i++) {
                double a0 = 22.5 + i * 45;
                double a1 = a0 + 45;
                Color glass = i % 2 == 0 ? light : mix(light, beacon, 0.35);
                b.flat(poly(cos(a0) * pane, sin(a0) * pane, cos(a1) * pane, sin(a1) * pane,
                        cos(a1) * 0.05, sin(a1) * 0.05, cos(a0) * 0.05, sin(a0) * 0.05), z, glass);
                b.line(open(cos(a0) * pane, sin(a0) * pane, cos(a0) * 0.05, sin(a0) * 0.05), z, gold.shade(), 0.01);
            }
            b.solid(ngon(0, 0, 4, 0.075, 0), z, 0.05, gold);
            if (tier == 3) {
                b.flat(circ(0, 0, 0.018), z + 0.05, Color.WHITE);
            }
        }
        b.top = z;
        return b;
    }
}
