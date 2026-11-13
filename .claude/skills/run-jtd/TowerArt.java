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
import java.util.function.DoubleFunction;

/**
 * Throwaway art lab for the next-gen tower looks (docs/features/next-gen-art): every tower as a list of
 * flat vector parts in cell units, each with a height, so one definition paints both art directions.
 * Nothing here is game code; TowerArtSheets renders the sheets.
 */
final class TowerArt {

    /** How a part's height shows: a bevel toward the light (plan view) or a wall below a lifted top (raised). */
    enum Dir {
        PLAN(-0.075, -0.075), RAISED(0.0, -0.42);

        final double ux;
        final double uy;

        Dir(double ux, double uy) {
            this.ux = ux;
            this.uy = uy;
        }
    }

    static final double[] GROW = {0.275, 0.315, 0.36, 0.40};
    static final double[] CONSTANT = {0.34, 0.34, 0.34, 0.34};
    static final double RIM = 0.024;
    static final double STEP_H = 0.07;

    static Dir dir = Dir.RAISED;
    static double[] radius = GROW;

    static final int SNIPER = 0xa6d674;
    static final int BURST = 0xe2746c;
    static final int STORM = 0x8496ff;
    static final int HEXER = 0xb88ae8;
    static final int MORTAR = 0xc39062;
    static final int SCORCHER = 0xea862a;
    static final int AERIE = 0x5cc4e2;
    static final int OBELISK = 0x8f86b8;
    static final int BEACON = 0xd8b866;
    static final int RUBY = 0xd0405a;
    static final int SHADE = 0xa6e8c8;
    static final int SOUL = 0x7a54d8;
    static final int UPGRADE = 0x6ee08a;
    static final Color PAPER_GAP = new Color(0x0d0e10);

    /** Tier colours for the Upgrades panel and the unlock ring: Tier 0, I, II, III. */
    static final int[][] TIERS = {
        {0xa6d674, 0x5cc8b0, 0x93dbcb, 0xc9e8ce},
        {0xe2746c, 0xe8938c, 0xf0b8b3, 0xf8d6c2},
        {0x8496ff, 0x8496ff, 0xc0c9ff, 0xe0dee8},
        {0xb88ae8, 0xb88ae8, 0xdbc3f3, 0xeddce2},
        {0xc39062, 0xe0b070, 0xebcba1, 0xf5e0b8},
        {0xea862a, 0xf0a040, 0xf5c081, 0xfadaa8},
        {0x5cc4e2, 0x8fd0f2, 0xb5e0f6, 0xdaeae3},
        {0x8f86b8, 0xa89fd2, 0xc6c0e1, 0xe2dad8},
        {0xd8b866, 0xecd48c, 0xf2e3b3, 0xf8ecc2},
    };

    private TowerArt() {
    }

    // ---- colour ----------------------------------------------------------------------------------------------

    record Ramp(Color lo, Color mid, Color hi) {
        static Ramp of(int rgb) {
            Color c = new Color(rgb);
            return new Ramp(mix(c, Color.BLACK, 0.45), c, mix(c, Color.WHITE, 0.38));
        }

        static Ramp of(Color c) {
            return of(c.getRGB() & 0xffffff);
        }
    }

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

    /** The plinth stone of a tower: one grey, leaning a little toward the tower's colour. */
    static Ramp stone(int rgb) {
        return Ramp.of(mix(new Color(0x50535e), new Color(rgb), 0.24));
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

    // ---- parts -----------------------------------------------------------------------------------------------

    enum Kind { SOLID, FLAT, GLOW, LINE }

    record Part(Kind kind, Shape shape, double z0, double h, Ramp ramp, Color color, double width) {
    }

    /** A tower under construction: parts from the ground up, in paint order. */
    static final class Build {
        final List<Part> parts = new ArrayList<>();
        /** What stands south of the tower's centre: painted last, over everything behind it. */
        final List<Part> front = new ArrayList<>();
        /** Where the moving part stands: the top of the foundation. */
        double top;
        /** How high the Aerie's nest circles. */
        double nest;
        double bodyRadius;
        double footprint;

        Build solid(Shape s, double z0, double h, Ramp r) {
            this.parts.add(new Part(Kind.SOLID, s, z0, h, r, null, 0));
            return this;
        }

        Build flat(Shape s, double z, Color c) {
            this.parts.add(new Part(Kind.FLAT, s, z, 0, null, c, 0));
            return this;
        }

        Build glow(double x, double y, double r, double z, Color c) {
            this.parts.add(new Part(Kind.GLOW, circ(x, y, r), z, 0, null, c, 0));
            return this;
        }

        Build line(Shape s, double z, Color c, double w) {
            this.parts.add(new Part(Kind.LINE, s, z, 0, null, c, w));
            return this;
        }

        /**
         * Parts that stand around the tower's centre. Those north of it stay where they are in the paint order, behind
         * the centre; those south of it are painted last; one that straddles the centre is cut in two.
         */
        Build around(Runnable block) {
            int from = this.parts.size();
            block.run();
            return this.around(from);
        }

        /** As {@link #around(Runnable)}, for the parts added since {@code from}. */
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
                    this.parts.add(new Part(p.kind(), north, p.z0(), p.h(), p.ramp(), p.color(), p.width()));
                    this.front.add(new Part(p.kind(), south, p.z0(), p.h(), p.ramp(), p.color(), p.width()));
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

        /** How many draw records the frame would carry for this tower, were every part its own record. */
        int count() {
            return this.parts.size();
        }
    }

    /** What the moving part shows. */
    static final class State {
        double aim = -35;
        /** 1 on the tick it fired, easing to 0. */
        double fire;
        /** 0 just fired, 1 ready. */
        double reload = 1;
        int stacks;
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
    }

    // ---- painting --------------------------------------------------------------------------------------------

    /** Paints a tower centred on {@code cx, cy}, a cell {@code px} pixels wide. */
    static void paint(Graphics2D g, Build b, double cx, double cy, double px) {
        AffineTransform save = g.getTransform();
        g.translate(cx, cy);
        g.scale(px, px);
        paintCell(g, b, px);
        g.setTransform(save);
    }

    /** Paints a tower at 0,0 of a pen already in cell units; {@code px} only sets how finely walls are swept. */
    static void paintCell(Graphics2D g, Build b, double px) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fill(ell(b.footprint * 0.14, b.footprint * 0.16, b.footprint * 1.06, b.footprint * 1.02));
        for (Part p : b.parts) {
            paintPart(g, p, px);
        }
    }

    static void paintPart(Graphics2D g, Part p, double px) {
        double x0 = dir.ux * p.z0();
        double y0 = dir.uy * p.z0();
        double x1 = dir.ux * (p.z0() + p.h());
        double y1 = dir.uy * (p.z0() + p.h());
        switch (p.kind()) {
            case SOLID -> {
                int n = Math.max(1, (int) Math.ceil(Math.hypot(x1 - x0, y1 - y0) * px / 0.5));
                g.setColor(p.ramp().lo());
                for (int i = 0; i <= n; i++) {
                    double t = (double) i / n;
                    g.fill(mv(p.shape(), x0 + (x1 - x0) * t, y0 + (y1 - y0) * t));
                }
                Shape top = mv(p.shape(), x1, y1);
                g.setColor(p.ramp().hi());
                g.fill(top);
                Shape clip = g.getClip();
                g.clip(top);
                g.setColor(p.ramp().mid());
                g.fill(mv(top, RIM, RIM));
                g.setClip(clip);
            }
            case FLAT -> {
                g.setColor(p.color());
                g.fill(mv(p.shape(), x0, y0));
            }
            case GLOW -> {
                Rectangle2D r = p.shape().getBounds2D();
                Color c = p.color();
                g.setPaint(new RadialGradientPaint((float) (r.getCenterX() + x0), (float) (r.getCenterY() + y0),
                        (float) (r.getWidth() / 2), new float[] {0f, 1f}, new Color[] {c, alpha(c, 0)}));
                g.fill(mv(p.shape(), x0, y0));
            }
            case LINE -> {
                g.setColor(p.color());
                g.setStroke(new BasicStroke((float) p.width(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(mv(p.shape(), x0, y0));
            }
            default -> throw new IllegalStateException();
        }
    }

    /** The shared tier cue: one plinth step per tier, in the tower's own plan shape. */
    static Build plinth(int tier, int rgb, DoubleFunction<Shape> shape) {
        Build b = new Build();
        Ramp stone = stone(rgb);
        double r = radius[tier];
        double step = radius == GROW ? 0.045 : 0.03;
        for (int i = 0; i < tier; i++) {
            b.solid(shape.apply(r - i * step), i * STEP_H, STEP_H, stone);
        }
        if (tier > 0) {
            b.flat(shape.apply(r - tier * step + step * 0.45), tier * STEP_H, mix(col(rgb), col(0x101014), 0.66));
        }
        b.footprint = r;
        b.bodyRadius = r - tier * step;
        b.top = tier * STEP_H;
        return b;
    }

    static Build build(String tower, int tier, State s) {
        return raw(tower, tier, s).finish();
    }

    private static Build raw(String tower, int tier, State s) {
        return switch (tower) {
            case "sniper" -> sniper(tier, s);
            case "burst" -> tier == 0 ? thrower(s) : stormcaller(tier, s);
            case "stormcaller" -> stormcaller(Math.max(1, tier), s);
            case "hexer" -> hexer(Math.max(1, tier), s);
            case "mortar" -> mortar(tier, s);
            case "scorcher" -> scorcher(tier, s);
            case "soulfire" -> scorcher(tier, s.look("soulfire"));
            case "aerie" -> aerie(tier, s);
            case "ruby" -> aerie(tier, s.look("ruby"));
            case "shade" -> aerie(tier, s.look("shade"));
            case "obelisk" -> obelisk(tier, s);
            case "beacon" -> beacon(tier, s);
            default -> throw new IllegalArgumentException(tower);
        };
    }

    // ---- Sniper ----------------------------------------------------------------------------------------------

    static Build sniper(int tier, State s) {
        Ramp green = Ramp.of(SNIPER);
        Ramp deck = Ramp.of(mix(col(SNIPER), col(0x16241a), 0.66));
        Ramp under = Ramp.of(mix(col(SNIPER), col(0x16241a), 0.38));
        Build b = plinth(tier, SNIPER, r -> ngon(0, 0, 6, r, 0));
        double r = b.bodyRadius;
        double z = b.top;
        if (tier >= 2) {
            b.solid(ngon(0, 0, 3, r, 90), z, 0.10, under);
            z += 0.10;
        }
        b.solid(ngon(0, 0, 3, r, -90), z, 0.16, green);
        z += 0.16;
        b.flat(ngon(0, 0.005, 3, r * 0.62, -90), z, deck.mid());
        if (tier >= 1) {
            {
                int mark = b.parts.size();
                for (int i = 0; i < 3; i++) {
                    b.solid(at(circ(0, 0, 0.04), -90 + 120 * i, r * 0.86), z, 0.10, Ramp.of(green.hi()));
                }
                b.around(mark);
            }
        }
        if (tier >= 3) {
            {
                int mark = b.parts.size();
                for (int i = 0; i < 3; i++) {
                    b.solid(at(circ(0, 0, 0.036), 90 + 120 * i, r * 0.86), z - 0.16, 0.14, Ramp.of(green.hi()));
                }
                b.around(mark);
            }
            b.solid(ngon(0, 0, 6, r * 0.40, 0), z, 0.10, green);
            z += 0.10;
            b.flat(ngon(0, 0, 6, r * 0.27, 0), z, deck.mid());
        }
        b.top = z;
        sniperHead(b, s);
        return b;
    }

    static void sniperHead(Build b, State s) {
        double z = b.top;
        Ramp steel = Ramp.of(mix(col(SNIPER), Color.WHITE, 0.55));
        Ramp body = Ramp.of(SNIPER);
        double len = s.has("long") ? 0.37 : 0.30;
        double back = s.fire * 0.05;
        double shake = s.has("frenzy") ? 0.014 : 0;
        List<Part> head = new ArrayList<>();
        Build h = new Build();
        h.solid(circ(0, 0, 0.058), z, 0.03, Ramp.of(0x59626a));
        h.solid(box((0.04 + len) / 2 - back, shake, len - 0.04, 0.034), z + 0.03, 0.03, steel);
        if (s.has("silver")) {
            h.solid(box(len - 0.05 - back, shake, 0.10, 0.04), z + 0.03, 0.034, Ramp.of(0xaad2ff));
            h.flat(circ(len - 0.03 - back, shake - 0.006, 0.012), z + 0.07, Color.WHITE);
        }
        if (s.has("hollow")) {
            h.solid(box(len * 0.55 - back, shake, 0.03, 0.058), z + 0.03, 0.036, Ramp.of(0xe69646));
        }
        h.solid(box(-0.015 - back, shake, 0.13, 0.072), z + 0.03, 0.04, body);
        h.solid(box(0.035 - back, -0.05 + shake, 0.10, 0.028), z + 0.05, 0.03, Ramp.of(0x59626a));
        double glint = 0.3 + 0.23 * Math.min(3, s.stacks);
        Color lens = mix(col(0x2c4030), col(0xf2fff0), glint);
        h.flat(circ(0.09 - back, -0.05 + shake, 0.017), z + 0.08, lens);
        if (s.stacks >= 3) {
            h.glow(0.09 - back, -0.05 + shake, 0.06, z + 0.08, alpha(0xf2fff0, 0.8));
            h.flat(star(0.09 - back, -0.05 + shake, 4, 0.042, 0.008, 0), z + 0.08, Color.WHITE);
        }
        if (s.has("headhunter")) {
            h.line(circ(0.09 - back, -0.05 + shake, 0.04), z + 0.08, col(0xff7850), 0.014);
            h.line(open(0.09 - back - 0.06, -0.05 + shake, 0.09 - back + 0.06, -0.05 + shake), z + 0.08,
                    col(0xff7850), 0.01);
        }
        if (s.has("fourth")) {
            h.glow(len - back, shake, 0.085, z + 0.07, alpha(Color.WHITE, 0.85));
            h.flat(ngon(len - back, shake, 4, 0.03, 0), z + 0.07, Color.WHITE);
        }
        if (s.has("frenzy")) {
            Color spark = col(0xff6b5a);
            h.line(open(-0.07, -0.05, -0.12, -0.09), z + 0.08, spark, 0.014);
            h.line(open(-0.08, 0.04, -0.14, 0.07), z + 0.08, spark, 0.014);
            h.line(open(0.0, 0.055, -0.02, 0.105), z + 0.08, spark, 0.014);
        }
        if (s.fire > 0.5) {
            h.glow(len + 0.02, 0, 0.09, z + 0.07, alpha(0xf4ffe8, 0.9));
        }
        for (Part p : h.parts) {
            head.add(new Part(p.kind(), rot(p.shape(), s.aim), p.z0(), p.h(), p.ramp(), p.color(), p.width()));
        }
        b.parts.addAll(head);
    }

    // ---- Burst: the stone thrower, and its two forks ---------------------------------------------------------

    static void stoneRing(Build b, double r, double z, int rgb, boolean two) {
        b.around(() -> stones(b, r, z, rgb, two));
    }

    private static void stones(Build b, double r, double z, int rgb, boolean two) {
        Ramp a = stone(rgb);
        Ramp c = Ramp.of(mix(a.mid(), Color.WHITE, 0.16));
        for (int i = 0; i < 9; i++) {
            double size = 0.058 + 0.012 * ((i * 7) % 3);
            b.solid(at(ngon(0, 0, 5 + i % 2, size, i * 31), i * 40, r), z, 0.09 + 0.02 * (i % 2), i % 2 == 0 ? a : c);
        }
        if (two) {
            for (int i = 0; i < 5; i++) {
                b.solid(at(ngon(0, 0, 5, 0.046, i * 47), 20 + i * 72, r * 0.98), z + 0.10, 0.07, i % 2 == 0 ? c : a);
            }
        }
    }

    static Build thrower(State s) {
        Ramp wood = Ramp.of(BURST);
        Build b = plinth(0, BURST, r -> circ(0, 0, r));
        double r = b.bodyRadius;
        b.flat(circ(0, 0, r * 0.74), 0, col(0x2b2422));
        stoneRing(b, r * 0.78, 0, BURST, true);
        b.solid(box(0, -0.075, 0.05, 0.045), 0, 0.14, wood);
        b.solid(box(0, 0.075, 0.05, 0.045), 0, 0.14, wood);
        b.solid(box(0, 0, 0.026, 0.17), 0.12, 0.02, Ramp.of(0x6a5a52));
        if (s.fire > 0.5) {
            b.solid(rot(box(0.045, 0, 0.09, 0.034), -40), 0.2, 0.03, wood);
            b.solid(at(circ(0, 0, 0.04), -40, 0.095), 0.26, 0.02, Ramp.of(wood.lo()));
        } else {
            b.solid(rot(box(0.085, 0, 0.17, 0.034), 140), 0.14, 0.03, wood);
            b.solid(at(circ(0, 0, 0.046), 140, 0.17), 0.14, 0.03, Ramp.of(wood.lo()));
            if (s.reload > 0.5) {
                b.solid(at(ngon(0, 0, 6, 0.032, 10), 140, 0.17), 0.17, 0.03, Ramp.of(0xd8c8c0));
            }
        }
        b.top = 0.2;
        return b;
    }

    static Build stormcaller(int tier, State s) {
        Ramp copper = Ramp.of(0xc98a5c);
        Ramp iron = Ramp.of(0x596074);
        Ramp blue = Ramp.of(STORM);
        Build b = plinth(tier, STORM, r -> ngon(0, 0, 12, r, 15));
        double r = b.bodyRadius;
        double z = b.top;
        if (tier == 1) {
            stoneRing(b, r * 0.80, z, STORM, false);
        } else {
            b.solid(ngon(0, 0, 8, r, 22.5), z, 0.05, iron);
            z += 0.05;
            for (int i = 0; i < 8; i++) {
                b.flat(at(circ(0, 0, 0.013), i * 45, r * 0.84), z, iron.hi());
            }
        }
        if (tier == 3) {
            {
                int mark = b.parts.size();
                for (int i = 0; i < 6; i++) {
                    b.solid(at(circ(0, 0, 0.042), 30 + i * 60, r * 0.74), z, 0.14, Ramp.of(0xdfe6f5));
                    b.flat(at(circ(0, 0, 0.018), 30 + i * 60, r * 0.74), z + 0.14, blue.mid());
                }
                b.around(mark);
            }
        }
        int turns = 2 + tier;
        double wide = 0.105 + 0.012 * tier;
        for (int i = 0; i < turns; i++) {
            double t = (double) i / (turns - 1);
            b.solid(circ(0, 0, wide - (wide - 0.06) * t), z, 0.085, copper);
            z += 0.085;
        }
        if (tier >= 2) {
            double crown = z - 0.05;
            b.around(() -> b.solid(ring(0, 0, wide + 0.025, wide - 0.012), crown, 0.03, blue));
        }
        if (tier >= 3) {
            b.solid(ring(0, 0, wide - 0.03, wide - 0.06), z + 0.04, 0.03, blue);
            z += 0.06;
        }
        boolean full = s.has("full");
        if (full) {
            b.glow(0, 0, 0.26, z, alpha(STORM, 0.75));
        }
        b.solid(circ(0, 0, 0.048), z, 0.05, full ? Ramp.of(0xe8eeff) : blue);
        if (s.fire > 0.5) {
            b.glow(0, 0, 0.2, z + 0.05, alpha(0xdfe8ff, 0.8));
            Random rnd = new Random(7);
            for (int i = 0; i < 4; i++) {
                double a = 20 + i * 90 + rnd.nextInt(30);
                b.line(open(cos(a) * 0.05, sin(a) * 0.05, cos(a + 22) * 0.11, sin(a + 22) * 0.11,
                        cos(a - 8) * 0.15, sin(a - 8) * 0.15, cos(a + 10) * 0.21, sin(a + 10) * 0.21), z + 0.05,
                        col(0xe6edff), 0.016);
            }
        }
        b.top = z;
        return b;
    }

    static Build hexer(int tier, State s) {
        Ramp iron = Ramp.of(0x555062);
        Ramp bone = Ramp.of(0xe6dcc4);
        Color brew = mix(col(HEXER), col(0x3a1c60), 0.35);
        Build b = plinth(tier, HEXER, r -> ngon(0, 0, 12, r, 15));
        double r = b.bodyRadius;
        double z = b.top;
        if (tier == 1) {
            stoneRing(b, r * 0.80, z, HEXER, false);
        }
        if (tier == 3) {
            b.line(circ(0, 0, r * 0.94), z, alpha(HEXER, 0.9), 0.014);
            for (int i = 0; i < 6; i++) {
                b.line(open(cos(i * 60) * r * 0.86, sin(i * 60) * r * 0.86, cos(i * 60) * r * 1.0, sin(i * 60) * r * 1.0),
                        z, alpha(HEXER, 0.9), 0.014);
            }
        }
        if (tier >= 2) {
            {
                int mark = b.parts.size();
                for (int i = 0; i < 3; i++) {
                    b.solid(rot(box(0.16, 0, 0.17, 0.034), 90 + i * 120), z, 0.05, iron);
                    b.solid(at(ngon(0, 0, 4, 0.052, 20), 30 + i * 120, r * 0.74), z, 0.12, stone(HEXER));
                    b.flat(at(box(0, 0, 0.012, 0.045), 30 + i * 120, r * 0.74), z + 0.12, Ramp.of(HEXER).hi());
                }
                b.around(mark);
            }
        }
        double pot = tier >= 2 ? 0.15 : 0.14;
        double lift = tier >= 2 ? 0.10 : 0;
        b.solid(circ(0, 0, pot), z + lift, 0.14, iron);
        z += lift + 0.14;
        boolean full = s.has("full");
        b.flat(circ(0, 0, pot * 0.8), z, full ? Ramp.of(HEXER).hi() : brew);
        if (full) {
            b.glow(0, 0, pot * 1.7, z, alpha(HEXER, 0.75));
        }
        Color bubble = Ramp.of(HEXER).hi();
        b.flat(circ(-0.045, -0.02, 0.026), z, bubble);
        b.flat(circ(0.04, 0.045, 0.018), z, bubble);
        b.flat(circ(0.05, -0.05, 0.012), z, bubble);
        if (s.fire > 0.5) {
            b.solid(circ(0.01, 0, 0.07), z, 0.04, Ramp.of(bubble));
            b.flat(circ(-pot * 1.1, -0.06, 0.02), z, bubble);
            b.flat(circ(pot * 0.95, -0.12, 0.016), z, bubble);
            b.flat(circ(0.05, pot * 1.2, 0.018), z, bubble);
        }
        if (tier == 3) {
            Shape tusk = minus(circ(0, 0, 0.105), circ(0.04, 0, 0.11));
            {
                int mark = b.parts.size();
                for (int i = 0; i < 3; i++) {
                    b.solid(rot(mv(tusk, -0.085, 0), 30 + i * 120), z + 0.05, 0.05, bone);
                }
                b.around(mark);
            }
        }
        b.top = z;
        return b;
    }

    // ---- Mortar ----------------------------------------------------------------------------------------------

    static Build mortar(int tier, State s) {
        Ramp iron = Ramp.of(0x5a6070);
        Ramp brown = Ramp.of(MORTAR);
        Ramp sand = Ramp.of(0xb5a27c);
        Build b = plinth(tier, MORTAR, r -> ngon(0, 0, 8, r, 22.5));
        double r = b.bodyRadius;
        double z = b.top;
        if (tier == 3) {
            {
                int mark = b.parts.size();
                b.solid(minus(ngon(0, 0, 8, r * 1.02, 22.5), ngon(0, 0, 8, r * 0.78, 22.5)), z, 0.16, stone(MORTAR));
                for (int i = 0; i < 8; i++) {
                    b.solid(at(box(0, 0, 0.05, 0.06), 22.5 + i * 45, r * 0.9), z + 0.16, 0.05,
                            Ramp.of(stone(MORTAR).hi()));
                }
                b.around(mark);
            }
            b.flat(ngon(0, 0, 8, r * 0.78, 22.5), z, col(0x2a2622));
            {
                int mark = b.parts.size();
                for (int i = 0; i < 3; i++) {
                    shell(b, "normal", cos(118 + i * 22) * r * 0.62, sin(118 + i * 22) * r * 0.62, 0.028, z);
                }
                b.around(mark);
            }
        } else if (tier >= 1) {
            {
                int mark = b.parts.size();
                for (int i = 0; i < 10; i++) {
                    b.solid(at(ell(0, 0, 0.042, 0.066), i * 36, r * 0.84), z, 0.07, sand);
                }
                b.around(mark);
            }
        }
        double base = tier == 0 ? r * 0.8 : r * 0.6;
        b.solid(circ(0, 0, base), z, 0.08, tier >= 2 ? iron : brown);
        z += 0.08;
        if (tier >= 2) {
            b.flat(ring(0, 0, base * 0.82, base * 0.66), z, brown.mid());
            for (int i = 0; i < 8; i++) {
                b.flat(at(circ(0, 0, 0.011), i * 45, base * 0.9), z, iron.hi());
            }
        } else {
            b.solid(circ(0, 0, base * 0.68), z, 0.02, iron);
        }
        b.top = z;
        mortarHead(b, s);
        return b;
    }

    static void mortarHead(Build b, State s) {
        double z = b.top + 0.02;
        Ramp brown = Ramp.of(mix(col(MORTAR), Color.WHITE, 0.12));
        Ramp iron = Ramp.of(0x6a7080);
        double len = (s.has("long") ? 0.25 : 0.21) - s.fire * 0.05;
        Build h = new Build();
        h.solid(box(0, 0, 0.055, 0.2), z, 0.05, iron);
        h.solid(box(len / 2 - 0.04, 0, len + 0.04, 0.135), z, 0.10, brown);
        h.solid(box(len - 0.015, 0, 0.04, 0.16), z, 0.11, Ramp.of(brown.hi()));
        h.flat(ell(len - 0.05, 0, 0.042, 0.05), z + 0.11, col(0x17130f));
        if (s.has("volley")) {
            h.solid(box(-0.10, 0, 0.05, 0.17), z, 0.04, iron);
        }
        Build shells = new Build();
        if (s.reload >= 0.5) {
            shell(shells, s.shell, len - 0.05, 0, 0.034, z + 0.11);
            if (s.has("volley")) {
                for (int i = -1; i <= 1; i++) {
                    shell(shells, s.shell, -0.10, i * 0.052, 0.022, z + 0.04);
                }
            }
        }
        if (s.fire > 0.5) {
            h.glow(len + 0.02, 0, 0.13, z + 0.11, alpha(0xffe6b0, 0.9));
        }
        h.parts.addAll(shells.parts);
        for (Part p : h.parts) {
            b.parts.add(new Part(p.kind(), rot(p.shape(), s.aim), p.z0(), p.h(), p.ramp(), p.color(), p.width()));
        }
    }

    /** One shell kind: the same drawing in the barrel, in the pane's magazine and in flight. */
    static void shell(Build b, String kind, double x, double y, double r, double z) {
        switch (kind) {
            case "heavy" -> {
                b.solid(circ(x, y, r * 1.25), z, 0.02, Ramp.of(0x7b8496));
                b.flat(box(x, y, r * 2.3, r * 0.5), z + 0.02, col(0xf2d28a));
            }
            case "nuke" -> {
                b.glow(x, y, r * 3.2, z, alpha(0xc8f04a, 0.7));
                b.solid(circ(x, y, r * 1.5), z, 0.02, Ramp.of(0xe8e8dc));
                b.flat(ngon(x, y, 3, r * 0.9, -90), z + 0.02, col(0x2a2a20));
            }
            case "napalm" -> {
                b.solid(circ(x, y, r), z, 0.02, Ramp.of(0xff7a2a));
                b.flat(circ(x - r * 0.2, y - r * 0.2, r * 0.4), z + 0.02, col(0xffd34d));
            }
            case "tar" -> {
                b.solid(circ(x, y, r), z, 0.02, new Ramp(col(0x4b3c5a), col(0x241d2a), col(0x6a5a80)));
                b.flat(circ(x - r * 0.3, y - r * 0.35, r * 0.28), z + 0.02, col(0xf0eaff));
            }
            case "cryo" -> {
                b.solid(circ(x, y, r), z, 0.02, Ramp.of(0xaae6ff));
                b.flat(star(x, y, 4, r * 0.7, r * 0.2, 0), z + 0.02, Color.WHITE);
            }
            case "flare" -> {
                b.glow(x, y, r * 2.4, z, alpha(0xfff2a8, 0.8));
                b.flat(star(x, y, 4, r * 1.2, r * 0.32, 45), z + 0.02, col(0xfffbe0));
            }
            default -> {
                b.solid(circ(x, y, r), z, 0.02, Ramp.of(0x7b8496));
                b.flat(circ(x - r * 0.3, y - r * 0.3, r * 0.3), z + 0.02, col(0xc4cad6));
            }
        }
    }

    // ---- Scorcher --------------------------------------------------------------------------------------------

    static Build scorcher(int tier, State s) {
        boolean soul = s.look.equals("soulfire");
        Ramp metal = soul ? new Ramp(col(0x1f1b2a), col(0x3d3650), col(0x8a78c0)) : Ramp.of(SCORCHER);
        Ramp iron = Ramp.of(soul ? 0x3a3648 : 0x565a66);
        Color hot = soul ? col(0xb9a0ff) : col(0xffb347);
        Build b = plinth(tier, soul ? SOUL : SCORCHER, r -> ngon(0, 0, 16, r, 0));
        double r = b.bodyRadius;
        double z = b.top;
        boolean streaming = s.has("stream");
        if (tier == 0) {
            b.solid(circ(0, 0, r * 0.74), z, 0.16, metal);
            b.line(circ(0, 0, r * 0.5), z + 0.16, metal.lo(), 0.016);
            z += 0.16;
        } else if (tier == 1) {
            b.solid(circ(-0.15, 0.13, 0.085), z, 0.12, metal);
            b.line(circ(-0.15, 0.13, 0.05), z + 0.12, metal.lo(), 0.014);
            b.solid(circ(0.02, -0.02, r * 0.62), z, 0.16, metal);
            b.line(circ(0.02, -0.02, r * 0.42), z + 0.16, metal.lo(), 0.016);
            b.line(open(-0.15, 0.13, -0.16, 0.0, -0.06, -0.04), z + 0.16, col(0x2c2c32), 0.03);
            z += 0.16;
        } else if (tier == 2) {
            double g = r * 0.80;
            b.flat(box(0, 0, g * 2, g * 2), z, soul ? col(0x2a1c4a) : col(0x5a1e10));
            for (int i = 0; i < 5; i++) {
                b.flat(circ(-g * 0.7 + i * g * 0.35, (i % 2 == 0 ? -1 : 1) * g * 0.72, 0.022), z, hot);
            }
            b.solid(minus(box(0, 0, g * 2.1, g * 2.1), box(0, 0, g * 1.74, g * 1.74)), z, 0.07, iron);
            for (int i = -1; i <= 1; i++) {
                b.line(open(i * g * 0.5, -g * 0.9, i * g * 0.5, g * 0.9), z + 0.07, iron.mid(), 0.022);
            }
            b.solid(circ(-g * 0.62, g * 0.62, 0.075), z + 0.07, 0.11, metal);
            b.solid(circ(g * 0.62, g * 0.62, 0.075), z + 0.07, 0.11, metal);
            b.solid(circ(0, -0.02, 0.135), z + 0.07, 0.15, metal);
            b.line(circ(0, -0.02, 0.09), z + 0.22, metal.lo(), 0.016);
            z += 0.22;
        } else {
            b.solid(ngon(0, 0, 8, r, 22.5), z, 0.22, iron);
            z += 0.22;
            b.line(ngon(0, 0, 8, r * 0.86, 22.5), z, metal.mid(), 0.028);
            for (int i = 0; i < 4; i++) {
                b.flat(at(box(0, 0, 0.045, 0.12), 45 + i * 90, r * 0.60), z,
                        streaming ? (soul ? col(0xd9c8ff) : col(0xffd34d)) : (soul ? col(0x4a3a80) : col(0x8a3418)));
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
        double len = (s.has("long") ? 0.31 : 0.25) + (s.has("dragon") ? 0.03 : 0);
        Ramp pipe = Ramp.of(metal.hi());
        Color tip = streaming ? col(0xfff2c0) : mix(soul ? col(0xb9a0ff) : col(0xff5a2a), iron.mid(), s.reload);
        Build h = new Build();
        h.solid(circ(0, 0, 0.066), z, 0.03, iron);
        h.solid(box(len / 2, 0, len, 0.05), z + 0.03, 0.03, pipe);
        h.solid(poly(len - 0.01, -0.028, len + 0.065, -0.055, len + 0.065, 0.055, len - 0.01, 0.028), z + 0.03, 0.035,
                Ramp.of(tip));
        if (!streaming) {
            h.flat(poly(len + 0.065, -0.014, len + 0.105, 0, len + 0.065, 0.014), z + 0.065,
                    soul ? col(0xd5e6ff) : col(0xffd34d));
        }
        for (Part p : h.parts) {
            b.parts.add(new Part(p.kind(), rot(p.shape(), s.aim), p.z0(), p.h(), p.ramp(), p.color(), p.width()));
        }
        return b;
    }

    /** The flame cone, in cell units from the nozzle at 0,0 along +x: three nested tongues, one record each. */
    static void stream(Graphics2D g, double range, double halfDeg, String look, int seed) {
        boolean soul = look.contains("soul");
        boolean white = look.contains("white");
        Color outer = soul ? alpha(0x5a36b8, 0.55) : alpha(0xe2402a, 0.55);
        Color middle = soul ? alpha(0x8f6cf0, 0.7) : alpha(0xff8a2a, 0.75);
        Color core = white ? alpha(0xffffff, 0.95) : soul ? alpha(0xd5e6ff, 0.9) : alpha(0xffe27a, 0.9);
        g.setColor(outer);
        g.fill(tongue(range, halfDeg, seed, 9));
        g.setColor(middle);
        g.fill(tongue(range * 0.78, halfDeg * 0.66, seed + 1, 7));
        g.setColor(core);
        g.fill(tongue(range * (look.contains("core") || white ? 0.72 : 0.42), halfDeg * 0.26, seed + 2, 5));
    }

    static Shape tongue(double range, double halfDeg, int seed, int tips) {
        Random rnd = new Random(seed);
        Path2D.Double p = new Path2D.Double();
        p.moveTo(0, 0.02);
        p.lineTo(0, -0.02);
        for (int i = 0; i <= tips * 2; i++) {
            double a = -halfDeg + 2 * halfDeg * i / (tips * 2);
            double edge = 1 - 0.10 * Math.abs(a / halfDeg);
            double r = range * edge * (i % 2 == 0 ? 0.78 + 0.1 * rnd.nextDouble() : 0.96 + 0.08 * rnd.nextDouble());
            p.lineTo(cos(a) * r, sin(a) * r);
        }
        p.closePath();
        return p;
    }

    // ---- Aerie -----------------------------------------------------------------------------------------------

    static Build aerie(int tier, State s) {
        boolean ruby = s.look.equals("ruby");
        boolean shade = s.look.equals("shade");
        int tint = ruby ? RUBY : AERIE;
        Ramp crystal = shade ? new Ramp(col(0x1d1f26), col(0x3c4050), col(0x7a8496)) : Ramp.of(tint);
        Ramp stone = shade ? Ramp.of(0x4a4e5a) : stone(tint);
        Color lit = shade ? col(SHADE) : Ramp.of(tint).hi();
        Build b = plinth(tier, shade ? 0x5a6470 : tint, r -> ngon(0, 0, 12, r, 15));
        double r = b.bodyRadius;
        double z = b.top;
        boolean release = s.has("release");
        b.solid(ngon(0, 0, 6, r * 0.78, 30), z, 0.08, shade ? stone : Ramp.of(mix(col(tint), col(0x10202c), 0.5)));
        z += 0.08;
        if (tier >= 2) {
            int shards = tier == 3 ? 6 : 4;
            {
                int mark = b.parts.size();
                for (int i = 0; i < shards; i++) {
                    double a = (tier == 3 ? 0 : 45) + i * 360.0 / shards;
                    b.solid(at(poly(-0.05, 0, 0, -0.026, 0.06, 0, 0, 0.026), a, r * 0.52), z, 0.13,
                            shade ? crystal : Ramp.of(Ramp.of(tint).hi()));
                }
                b.around(mark);
            }
        }
        if (tier >= 1) {
            if (release) {
                b.glow(0, 0, 0.30, z + 0.2, alpha(lit, 0.7));
            }
            {
                int mark = b.parts.size();
                b.solid(ring(0, 0, 0.185, 0.15), z + 0.2, 0.025, release ? Ramp.of(mix(lit, Color.WHITE, 0.5))
                        : shade ? Ramp.of(SHADE) : crystal);
                b.around(mark);
            }
        }
        if (tier == 3) {
            {
                int mark = b.parts.size();
                for (int i = 0; i < 3; i++) {
                    b.solid(rot(box(0.17, 0, 0.16, 0.03), -90 + i * 120), z + 0.3, 0.025, stone);
                    b.solid(at(ring(0, 0, 0.048, 0.026), -90 + i * 120, 0.25), z + 0.3, 0.03,
                            shade ? Ramp.of(SHADE) : crystal);
                }
                b.around(mark);
            }
        }
        b.nest = z + 0.2;
        double height = 0.45 + 0.15 * tier;
        if (tier == 3) {
            b.solid(star(0, 0, 6, 0.14, 0.09, 0), z, height, crystal);
        } else {
            b.solid(ngon(0, 0, 6, 0.10 + 0.008 * tier, 0), z, height, crystal);
        }
        z += height;
        if (shade) {
            for (int i = 0; i < 3; i++) {
                b.flat(rot(box(0.045, 0, 0.05, 0.012), 30 + i * 120), z, col(SHADE));
            }
            if (tier == 3) {
                b.glow(0, 0, 0.16, z, alpha(SHADE, 0.6));
                for (int i = 0; i < 6; i++) {
                    b.flat(at(poly(-0.02, -0.016, 0.03, 0, -0.02, 0.016), i * 60, 0.105), z, col(SHADE));
                }
            }
        }
        b.flat(ngon(0, 0, 6, 0.045, 30), z, shade ? col(SHADE) : mix(lit, Color.WHITE, 0.55));
        b.top = z;
        return b;
    }

    /** The nest: {@code count} stored missiles on one even ring, each the missile that will fly. */
    static void nest(Graphics2D g, int count, double phaseDeg, double ringRadius, String kind, boolean bright) {
        nest(g, count, phaseDeg, ringRadius, kind, bright, 0);
    }

    /** {@code half} -1 paints the missiles behind the spire and the ring's far arc, 1 those in front, 0 all. */
    static void nest(Graphics2D g, int count, double phaseDeg, double ringRadius, String kind, boolean bright, int half) {
        if (bright) {
            g.setColor(alpha(missileColour(kind), 0.55));
            g.setStroke(new BasicStroke(0.014f));
            if (half == 0) {
                g.draw(circ(0, 0, ringRadius));
            } else {
                g.draw(new java.awt.geom.Arc2D.Double(-ringRadius, -ringRadius, ringRadius * 2, ringRadius * 2, half < 0 ? 0 : 180, 180,
                        java.awt.geom.Arc2D.OPEN));
            }
        }
        for (int i = 0; i < count; i++) {
            double a = phaseDeg + 360.0 * i / count;
            if (half != 0 && (sin(a) >= 0) != (half > 0)) {
                continue;
            }
            AffineTransform save = g.getTransform();
            g.translate(cos(a) * ringRadius, sin(a) * ringRadius);
            g.rotate(Math.toRadians(a + 90));
            missile(g, kind, bright);
            g.setTransform(save);
        }
    }

    static Color missileColour(String kind) {
        return switch (kind) {
            case "ruby" -> col(RUBY);
            case "shade" -> col(SHADE);
            default -> col(AERIE);
        };
    }

    /** One missile heading +x, about a tenth of a cell long. */
    static void missile(Graphics2D g, String kind, boolean bright) {
        Color c = missileColour(kind);
        if (bright) {
            g.setPaint(new RadialGradientPaint(0f, 0f, 0.09f, new float[] {0f, 1f}, new Color[] {alpha(c, 0.8), alpha(c, 0)}));
            g.fill(circ(0, 0, 0.09));
        }
        switch (kind) {
            case "ruby" -> {
                g.setColor(mix(c, Color.BLACK, 0.35));
                g.fill(poly(0.07, 0, 0, 0.034, -0.05, 0, 0, -0.034));
                g.setColor(mix(c, Color.WHITE, 0.35));
                g.fill(poly(0.07, 0, 0, -0.034, -0.05, 0));
            }
            case "shade" -> {
                g.setColor(alpha(c, 0.35));
                g.fill(poly(0.0, -0.03, -0.11, 0, 0.0, 0.03));
                g.setColor(alpha(c, 0.9));
                g.fill(circ(0.01, 0, 0.032));
                g.setColor(col(0x1d1f26));
                g.fill(circ(0.018, 0, 0.013));
            }
            default -> {
                g.setColor(mix(c, Color.BLACK, 0.3));
                g.fill(poly(0.065, 0, -0.045, 0.036, -0.022, 0, -0.045, -0.036));
                g.setColor(mix(c, Color.WHITE, 0.5));
                g.fill(poly(0.065, 0, -0.022, 0, -0.045, -0.036));
            }
        }
    }

    // ---- Obelisk ---------------------------------------------------------------------------------------------

    static Build obelisk(int tier, State s) {
        Ramp dark = new Ramp(col(0x48446c), col(0x6a6490), col(0xa39ad0));
        Ramp stone = stone(OBELISK);
        Build b = plinth(tier, OBELISK, r -> box(0, 0, r * 1.72, r * 1.72));
        double z = b.top;
        double reach = (b.footprint - (radius == GROW ? 0.045 : 0.03) * 0.5) * 0.86 - 0.035;
        if (tier == 2) {
            {
                int mark = b.parts.size();
                for (int i = 0; i < 3; i++) {
                    b.solid(at(box(0, 0, 0.07, 0.07), -90 + i * 120, reach * 0.9), STEP_H, 0.2, dark);
                }
                b.around(mark);
            }
        }
        if (tier == 3) {
            {
                int mark = b.parts.size();
                for (int i = 0; i < 8; i++) {
                    double a = 22.5 + i * 45;
                    double d = reach / Math.max(Math.abs(cos(a)), Math.abs(sin(a))) * 0.93;
                    b.solid(mv(box(0, 0, 0.066, 0.066), cos(a) * d, sin(a) * d), STEP_H, 0.22, dark);
                }
                b.around(mark);
            }
        }
        double side = tier == 3 ? 0.30 : 0.26;
        double height = 0.45 + 0.12 * tier;
        b.solid(box(0, 0, side, side), z, height, dark);
        z += height;
        b.solid(box(0, 0, side * 0.86, side * 0.86), z, 0.2, dark);
        z += 0.2;
        double half = side * 0.43;
        b.flat(poly(0, 0, -half, -half, half, -half), z, mix(dark.hi(), Color.WHITE, 0.15));
        b.flat(poly(0, 0, -half, -half, -half, half), z, dark.hi());
        b.flat(poly(0, 0, half, -half, half, half), z, dark.mid());
        b.flat(poly(0, 0, -half, half, half, half), z, dark.lo());
        b.top = z;
        rune(b, s.look, z, s.has("full") ? 1.0 : 0.55, tier, s.fire > 0.5);
        return b;
    }

    static int runeColour(String look, int tier) {
        return switch (look) {
            case "burden" -> 0xc9a56c;
            case "gravity" -> 0x7d9be6;
            case "umbra" -> 0xbcaaf0;
            default -> TIERS[7][tier];
        };
    }

    /** The one glowing part of the stone. It brightens, never grows. */
    static void rune(Build b, String look, double z, double brightness, int tier, boolean flash) {
        Color c = col(runeColour(look, tier));
        Color ink = mix(c, Color.WHITE, flash ? 0.8 : brightness > 0.9 ? 0.45 : 0);
        b.glow(0, 0, 0.15, z, alpha(c, flash ? 1.0 : brightness * 0.9));
        double w = 0.016;
        int first = b.parts.size();
        switch (look) {
            case "burden" -> {
                b.line(open(-0.045, -0.04, 0.045, -0.04), z, ink, w);
                b.line(open(-0.04, -0.005, 0, 0.04, 0.04, -0.005), z, ink, w);
            }
            case "gravity" -> {
                Path2D.Double spiral = new Path2D.Double();
                for (int i = 0; i <= 24; i++) {
                    double a = i * 30;
                    double r = 0.052 - 0.045 * i / 24.0;
                    if (i == 0) {
                        spiral.moveTo(cos(a) * r, sin(a) * r);
                    } else {
                        spiral.lineTo(cos(a) * r, sin(a) * r);
                    }
                }
                b.line(spiral, z, ink, w * 0.8);
            }
            case "umbra" -> {
                b.line(circ(0, 0, 0.042), z, ink, w);
                b.flat(circ(0.012, 0.012, 0.036), z, col(0x2a2540));
            }
            default -> {
                b.line(open(0, -0.045, 0, 0.045), z, ink, w);
                b.line(open(-0.03, -0.012, 0.03, -0.012), z, ink, w);
            }
        }
        AffineTransform larger = AffineTransform.getScaleInstance(1.5, 1.5);
        for (int i = first; i < b.parts.size(); i++) {
            Part p = b.parts.get(i);
            b.parts.set(i, new Part(p.kind(), larger.createTransformedShape(p.shape()), p.z0(), p.h(), p.ramp(), p.color(), p.width() * 1.5));
        }
    }

    // ---- Beacon ----------------------------------------------------------------------------------------------

    static Build beacon(int tier, State s) {
        Ramp gold = Ramp.of(BEACON);
        Ramp stone = stone(BEACON);
        boolean day = s.has("daylight");
        Color light = day ? col(0xfffdf0) : col(0xffe9a8);
        Build b = plinth(tier, BEACON, r -> ngon(0, 0, 8, r, 22.5));
        double r = b.bodyRadius;
        double z = b.top;
        double post = tier >= 2 ? r * 0.8 : 0.14;
        double height = 0.22 + 0.13 * tier;
        b.solid(circ(0, 0, post), z, height, tier >= 2 ? Ramp.of(stone.hi()) : stone);
        z += height;
        if (tier == 3) {
            b.solid(circ(0, 0, post * 1.12), z, 0.03, stone);
            z += 0.03;
            for (int i = 0; i < 8; i++) {
                b.flat(at(circ(0, 0, 0.014), i * 45, post * 1.0), z, gold.hi());
            }
        }
        b.glow(0, 0, tier == 0 ? 0.25 : 0.30, z, alpha(light, day ? 0.75 : 0.5));
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
                Color glass = s.panes.isEmpty() ? (i % 2 == 0 ? light : mix(light, col(BEACON), 0.35))
                        : col(s.panes.get(i % s.panes.size()));
                b.flat(poly(cos(a0) * pane, sin(a0) * pane, cos(a1) * pane, sin(a1) * pane,
                        cos(a1) * 0.05, sin(a1) * 0.05, cos(a0) * 0.05, sin(a0) * 0.05), z, glass);
                b.line(open(cos(a0) * pane, sin(a0) * pane, cos(a0) * 0.05, sin(a0) * 0.05), z, gold.lo(), 0.01);
            }
            b.solid(ngon(0, 0, 4, 0.075, 0), z, 0.05, gold);
            if (tier == 3) {
                b.flat(circ(0, 0, 0.018), z + 0.05, Color.WHITE);
            }
        }
        b.top = z;
        return b;
    }

    // ---- shared marks ----------------------------------------------------------------------------------------

    /** The one "an upgrade is ready" mark, in cell units, for the cell's top-left corner. */
    static void upgradeArrow(Graphics2D g) {
        Shape arrow = poly(0, -0.085, 0.075, 0.0, 0.03, 0.0, 0.03, 0.075, -0.03, 0.075, -0.03, 0.0, -0.075, 0.0);
        g.setColor(new Color(0x0d0e10));
        g.setStroke(new BasicStroke(0.045f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(arrow);
        g.setColor(col(UPGRADE));
        g.fill(arrow);
    }

    static Line2D line(double x0, double y0, double x1, double y1) {
        return new Line2D.Double(x0, y0, x1, y1);
    }
}
