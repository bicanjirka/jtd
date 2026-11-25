package td.ui;

import td.ui.render.LoadedShell;
import td.ui.render.TowerLook;
import td.ui.render.TowerPose;
import td.ui.render.TowerSpriteDraw;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The art sheets' way into the game's own tower art (TowerFoundations, TowerSprites, TowerLooks), which is
 * package-private: the sheets draw every tower through here, so a sheet shows what the game paints.
 * Everything is in cell units: the pen stands at the tower's cell centre, one unit to a cell.
 */
public final class TowerArtGame {

    private TowerArtGame() {
    }

    /** The missile of {@code kind} ("frost", "ruby", "shade") heading +x, {@code length} cells long. */
    public static void paintMissile(Graphics2D g, String kind, double length, boolean bright) {
        MissileArt.paint(g, missile(kind), (float) length, bright);
    }

    /** The nest's {@code count} missiles round the pen's origin, as the game circles them over a tower's top. */
    public static void paintNest(Graphics2D g, int count, double phaseDegrees, double ringRadius, String kind,
                                 boolean bright) {
        TowerLifeArt.nest(g, new td.ui.render.NestDraw(0f, 0f, (float) ringRadius,
                java.util.Collections.nCopies(count, missile(kind)), Math.toRadians(phaseDegrees), 0.12f, bright));
    }

    /** The "an upgrade is ready" arrow round the pen's origin. */
    public static void paintUpgradeArrow(Graphics2D g) {
        Java2DFrameRenderer.paintUpgradeArrow(g, 1, Java2DFrameRenderer.colorFor(td.ui.render.Palette.TOWER_UPGRADE_READY));
    }

    private static td.ui.render.LoadedMissile missile(String kind) {
        return switch (kind) {
            case "ruby" -> td.ui.render.LoadedMissile.RUBY;
            case "shade" -> td.ui.render.LoadedMissile.SHADE;
            default -> td.ui.render.LoadedMissile.FROST;
        };
    }

    /** The tower with its shadow, its head turned to {@code aimDegrees} (0 east, -90 north). */
    public static void paint(Graphics2D g, TowerLook look, int tier, TowerPose pose, double aimDegrees) {
        TowerSprites.paintShadow(g, TowerSpriteDraw.still(look, tier, 0, 0, 0f, 0f, 0f), 1);
        TowerSprites.paint(g, look, tier, pose, 0f, 0f, 1, Math.toRadians(aimDegrees));
    }

    /**
     * One frame of the game's own moving picture of a tower: {@code rise} of the way from {@code fromTier} up to
     * {@code tier}, {@code build} of the way through settling onto its cell (1 for neither), its lights at
     * {@code seconds} on the wall clock.
     */
    public static void paintLive(Graphics2D g, TowerLook look, int fromTier, int tier, TowerPose pose, double aimDegrees,
                                 double rise, double build, double seconds) {
        TowerSpriteDraw draw = new TowerSpriteDraw(look, tier, pose, 0, 0, false, 0f, 0f, 0f,
                (float) Math.toRadians(aimDegrees), false, fromTier, (float) rise, (float) build, (float) seconds);
        TowerSprites.paintShadow(g, draw, 1);
        TowerSprites.paint(g, draw, 1);
    }

    /** The tower's icon, a cell wide, at the pen's origin. */
    public static void paintIcon(Graphics2D g, TowerLook look) {
        TowerIcons.paint(g, look, 1);
    }

    /** The Upgrades panel's thumbnail of a tier: the tower at rest, {@code cell} pixels to a cell, grey when locked. */
    public static void paintThumbnail(Graphics2D g, TowerLook look, int tier, double cell, boolean locked) {
        TowerSprites.paintStill(g, look, tier, cell, locked);
    }

    /** A shell of {@code kind} on its own, {@code r} across, at the pen's origin. */
    public static void paintShell(Graphics2D g, LoadedShell kind, double r, double pixels) {
        TowerSprites.paintParts(g, TowerFoundations.shell(kind, r), pixels);
    }

    /**
     * The game's flame from a nozzle at the pen's origin along +x, {@code range} cells long and {@code halfDegrees}
     * either side, a White Flame as hot as it is drawn; {@code flicker} picks its tips.
     */
    public static void paintStream(Graphics2D g, double range, double halfDegrees, boolean soulfire, boolean white,
                                   float coreHalfWidth, int flicker) {
        FlameArt.paint(g, new td.ui.render.FlameDraw(List.of(new td.ui.render.FlameDraw.Stream(0f, 0f, 0f,
                (float) range, (float) Math.toRadians(halfDegrees), white ? 1f : 0f, coreHalfWidth, soulfire,
                flicker)), List.of(), 1f, 0f));
    }

    /** The game's Flashover tongue from {@code fromX, fromY} to {@code toX, toY}, in cell units. */
    public static void paintFlashover(Graphics2D g, double fromX, double fromY, double toX, double toY, int flicker) {
        Java2DFrameRenderer.paintStream(g, TowerEffectFrameBuilder.flashover(false, (float) fromX, (float) fromY,
                (float) toX, (float) toY, flicker));
    }

    /** One lit part: {@code shape} standing {@code height} tall in the three tones of {@code body}. */
    public static void paintSolid(Graphics2D g, Shape shape, double height, Color body, double pixels) {
        TowerSprites.paintParts(g, List.of(TowerFoundations.Part.solid(shape, 0, height, Tones.of(body))), pixels);
    }

    public static int partCount(TowerLook look, int tier, TowerPose pose) {
        TowerFoundations.Foundation f = TowerFoundations.of(look, tier, pose);
        return f.parts().size() + f.head().size() + f.front().size();
    }

    public static double footprint(int tier) {
        return TowerFoundations.footprint(tier);
    }

    /** How high the tower's top stands, in cells of height. */
    public static double top(TowerLook look, int tier) {
        return TowerFoundations.of(look, tier).top();
    }

    /** How high the ring round the tower circles, in cells of height. */
    public static double nest(TowerLook look, int tier) {
        return TowerFoundations.of(look, tier).nest();
    }

    /** How far out along its aim a shot leaves the head, in cells. */
    public static double muzzle(TowerLook look, int tier, TowerPose pose) {
        return TowerFoundations.of(look, tier, pose).muzzle();
    }

    public static Color color(TowerLook look) {
        return TowerLooks.color(look);
    }

    public static Color tierColor(TowerLook look, int tier) {
        return TowerLooks.tierColor(look, tier);
    }

    /**
     * Every part in paint order as one line of text (kind, heights, colours, outline), the head turned to
     * {@code aimDegrees}: what two builds of the same tower are compared by.
     */
    public static List<String> dump(TowerLook look, int tier, TowerPose pose, double aimDegrees) {
        TowerFoundations.Foundation f = TowerFoundations.of(look, tier, pose);
        AffineTransform turn = AffineTransform.getRotateInstance(Math.toRadians(aimDegrees));
        List<String> lines = new ArrayList<>();
        for (TowerFoundations.Part part : f.parts()) {
            lines.add(line(part, part.shape()));
        }
        for (TowerFoundations.Part part : f.head()) {
            lines.add(line(part, turn.createTransformedShape(part.shape())));
        }
        for (TowerFoundations.Part part : f.front()) {
            lines.add(line(part, part.shape()));
        }
        return lines;
    }

    private static String line(TowerFoundations.Part part, Shape shape) {
        String paint = part.tones() != null
                ? hex(part.tones().shade()) + "/" + hex(part.tones().body()) + "/" + hex(part.tones().highlight())
                : hex(part.color());
        return String.format(Locale.ROOT, "%s z=%.5f h=%.5f w=%.5f %s %s", part.kind(), part.z0(), part.height(),
                part.width(), paint, outline(shape));
    }

    public static String hex(Color c) {
        return String.format("%08x", c.getRGB());
    }

    public static String outline(Shape shape) {
        StringBuilder out = new StringBuilder();
        double[] c = new double[6];
        for (PathIterator it = shape.getPathIterator(null); !it.isDone(); it.next()) {
            int type = it.currentSegment(c);
            int n = type == PathIterator.SEG_CLOSE ? 0 : type == PathIterator.SEG_QUADTO ? 4
                    : type == PathIterator.SEG_CUBICTO ? 6 : 2;
            out.append(type);
            for (int i = 0; i < n; i++) {
                out.append(String.format(Locale.ROOT, ",%.4f", c[i]));
            }
            out.append(';');
        }
        return out.toString();
    }
}
