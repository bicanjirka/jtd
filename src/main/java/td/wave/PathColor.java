package td.wave;

/**
 * A path's own on-board color, as plain 0-255 RGB. Carries no {@code java.awt} dependency, so it
 * is exactly as safe to reference from {@code td.ui.render} as any other domain type - see
 * {@code td.ui.render.CellDraw}'s own reference to {@code td.cell.Cell}. {@link #DEFAULT} is
 * today's hardcoded white, so a path that never calls {@code PathDefinition.withColor} looks
 * exactly as every path always has.
 */
public record PathColor(int r, int g, int b) {

    public static final PathColor DEFAULT = new PathColor(255, 255, 255);

    public PathColor {
        r = clamp(r);
        g = clamp(g);
        b = clamp(b);
    }

    public static PathColor of(int r, int g, int b) {
        return new PathColor(r, g, b);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
