package td.wave;

/**
 * A path's colour as plain RGB, so render commands can carry it without AWT. {@link #DEFAULT} is
 * white.
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
