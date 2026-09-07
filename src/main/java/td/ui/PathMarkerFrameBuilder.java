package td.ui;

import td.ui.render.Palette;
import td.ui.render.PathMarkerDraw;
import td.ui.render.PathMarkerShape;
import td.wave.Path;
import td.wave.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes the path as two layers of {@link PathMarkerDraw}s: a static trail and a moving
 * indicator, both computed from {@link Path}'s real pixel-space points rather than baked into
 * any art asset - so the visual always matches whatever path a level actually defines,
 * diagonal segments included (nothing here assumes axis-aligned steps, unlike
 * {@code AbstractEnemyMob}'s per-segment fixed-point counter).
 * <p>
 * Both layers run through the same arc-length placement code, driven only by {@link MarkerStyle}
 * below - swapping either layer's symbol, spacing, size, or brightness role is a one-line change
 * to one of the two constants, and every marker carries a facing angle regardless of shape, so a
 * static dot can become a direction-sensitive symbol without touching this class further.
 */
final class PathMarkerFrameBuilder {

    // 50% of a baseline enemy's on-screen pace at TickSpeed.NORMAL: a mob advances
    // speedBase / 1000 = 0.04 of one cell per tick (AbstractEnemyMob.speedBase), and NORMAL
    // runs 1e9 / BASE_TICK_NANOS = 20 ticks/s (GameLoop), giving 0.8 cell/s; half of that is
    // 0.4. Kept as its own named constant rather than reaching into those classes - a
    // decorative overlay should not couple itself to simulation internals - and is a fixed
    // cells/second pace: see GameLoop.animationSeconds() for how (not) game speed feeds in.
    private static final float MOVING_CELLS_PER_SECOND = 0.4f;

    private static final MarkerStyle STATIC =
            MarkerStyle.of(PathMarkerShape.DOT, Palette.PATH_MARKER_STATIC, 0.5f, 0.08f);
    private static final MarkerStyle MOVING =
            MarkerStyle.of(PathMarkerShape.CHEVRON, Palette.PATH_MARKER_MOVING, 1.5f, 0.22f);

    private PathMarkerFrameBuilder() {
    }

    static List<PathMarkerDraw> build(Path path, int scale, double animationSeconds) {
        Polyline polyline = Polyline.of(path);
        if (polyline == null) {
            return List.of();
        }
        List<PathMarkerDraw> draws = new ArrayList<>();
        addLayer(draws, polyline, scale, STATIC, 0.0);
        addLayer(draws, polyline, scale, MOVING, animationSeconds * MOVING_CELLS_PER_SECOND * scale);
        return draws;
    }

    private static void addLayer(List<PathMarkerDraw> draws, Polyline polyline, int scale, MarkerStyle style, double offset) {
        float targetSpacing = style.spacingCells() * scale;
        int count = Math.max(1, Math.round(polyline.totalLength() / targetSpacing));
        // Dividing the exact total length by the marker count (rather than using
        // targetSpacing directly) keeps spacing uniform *and* closes the loop with no seam
        // at the wrap point.
        double spacing = polyline.totalLength() / (double) count;
        float size = style.sizeCells() * scale;
        for (int i = 0; i < count; i++) {
            Pose pose = polyline.poseAt(mod(i * spacing + offset, polyline.totalLength()));
            draws.add(new PathMarkerDraw(style.shape(), style.palette(), pose.x(), pose.y(), pose.facingRadians(), size));
        }
    }

    private static double mod(double value, double modulus) {
        double remainder = value % modulus;
        return remainder < 0 ? remainder + modulus : remainder;
    }

    /** One layer's symbol, spacing, size, and colour role - see the STATIC/MOVING constants above. */
    private record MarkerStyle(PathMarkerShape shape, Palette palette, float spacingCells, float sizeCells) {
        static MarkerStyle of(PathMarkerShape shape, Palette palette, float spacingCells, float sizeCells) {
            return new MarkerStyle(shape, palette, spacingCells, sizeCells);
        }
    }

    private record Pose(float x, float y, double facingRadians) {
    }

    /**
     * The path's real pixel-space points (already scaled and cell-centered by
     * {@link Path#getStep}) plus the cumulative distance travelled to reach each one, so any
     * distance along the path resolves to an exact position and facing by linear interpolation
     * within the segment it falls in.
     */
    private static final class Polyline {
        private final float[] xs;
        private final float[] ys;
        private final float[] cumulative;
        private final float totalLength;

        private Polyline(float[] xs, float[] ys, float[] cumulative, float totalLength) {
            this.xs = xs;
            this.ys = ys;
            this.cumulative = cumulative;
            this.totalLength = totalLength;
        }

        /** Returns {@code null} for a path with fewer than two points, or zero length. */
        static Polyline of(Path path) {
            int n = path.length();
            if (n < 2) {
                return null;
            }
            float[] xs = new float[n];
            float[] ys = new float[n];
            for (int i = 0; i < n; i++) {
                Point p = path.getStep(i);
                xs[i] = p.x();
                ys[i] = p.y();
            }
            float[] cumulative = new float[n];
            for (int i = 1; i < n; i++) {
                cumulative[i] = cumulative[i - 1] + (float) Math.hypot(xs[i] - xs[i - 1], ys[i] - ys[i - 1]);
            }
            float totalLength = cumulative[n - 1];
            if (totalLength <= 0f) {
                return null;
            }
            return new Polyline(xs, ys, cumulative, totalLength);
        }

        float totalLength() {
            return this.totalLength;
        }

        Pose poseAt(double distance) {
            int segment = this.cumulative.length - 2;
            for (int i = 0; i < this.cumulative.length - 1; i++) {
                if (distance < this.cumulative[i + 1]) {
                    segment = i;
                    break;
                }
            }
            float segmentLength = this.cumulative[segment + 1] - this.cumulative[segment];
            double t = segmentLength == 0f ? 0.0 : (distance - this.cumulative[segment]) / segmentLength;
            float x = lerp(this.xs[segment], this.xs[segment + 1], t);
            float y = lerp(this.ys[segment], this.ys[segment + 1], t);
            double facing = Math.atan2(this.ys[segment + 1] - this.ys[segment], this.xs[segment + 1] - this.xs[segment]);
            return new Pose(x, y, facing);
        }

        private static float lerp(float from, float to, double t) {
            return (float) (from + (to - from) * t);
        }
    }
}
