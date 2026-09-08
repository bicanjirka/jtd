package td.wave;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pure geometry: which grid cells does a pixel-space path's corridor cover "significantly"?
 * This is what makes buildability depend on the *final* path geometry - smoothed or not -
 * rather than a fixed list of grid cells the path happened to be authored through.
 * <p>
 * Deliberately takes {@code scale}/{@code width}/{@code height} rather than a {@code Cell[][]}:
 * a cell's own pixel bounds are recomputed here from its grid index the same way
 * {@code GameEngine.loadLevel} originally derived them ({@code (i*scale, j*scale, scale,
 * scale)}), rather than asking the {@code Cell} object - which has no notion of its own size
 * today. That keeps this class (and its tests) independent of the {@code Cell}/{@code
 * PathNormal} machinery entirely: a polyline and some dimensions in, a set of covered cells out.
 */
public final class PathCoverage {

    // One cell wide, matching the corridor width a raw (unsmoothed) grid path implicitly has
    // today: every step is exactly one cell, so the "corridor" was always just the cell itself.
    private static final double PATH_WIDTH_CELLS = 1.0;
    // A straight run's cells get exactly 100% coverage under a one-cell-wide corridor, and a
    // corner cell (where two perpendicular corridors overlap) gets ~94.6% - the corridor's
    // rounded end-caps don't quite reach the cell's far diagonal corner. 0.5 comfortably marks
    // both as covered without false-positiving on a curve that only clips a cell's corner.
    private static final double COVERAGE_THRESHOLD = 0.5;
    private static final int SAMPLES_PER_AXIS = 4;

    private PathCoverage() {
    }

    public static Set<Point> unbuildableCells(List<Vec2> polyline, int scale, int width, int height) {
        Set<Point> covered = new HashSet<>();
        if (polyline.size() < 2 || width <= 0 || height <= 0) {
            return covered;
        }

        double halfWidth = PATH_WIDTH_CELLS * scale / 2.0;
        double minX = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (Vec2 p : polyline) {
            minX = Math.min(minX, p.x());
            maxX = Math.max(maxX, p.x());
            minY = Math.min(minY, p.y());
            maxY = Math.max(maxY, p.y());
        }

        // Cells outside this box are never checked at all - not just an optimisation: it's
        // what keeps this safe to call against a grid where only some cells actually exist.
        int minCellX = clamp((int) Math.floor((minX - halfWidth) / scale), 0, width - 1);
        int maxCellX = clamp((int) Math.floor((maxX + halfWidth) / scale), 0, width - 1);
        int minCellY = clamp((int) Math.floor((minY - halfWidth) / scale), 0, height - 1);
        int maxCellY = clamp((int) Math.floor((maxY + halfWidth) / scale), 0, height - 1);

        for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
            for (int cellY = minCellY; cellY <= maxCellY; cellY++) {
                double coverage = coverageFraction(polyline, cellX * (double) scale, cellY * (double) scale, scale, halfWidth);
                if (coverage >= COVERAGE_THRESHOLD) {
                    covered.add(new Point(cellX, cellY));
                }
            }
        }
        return covered;
    }

    private static double coverageFraction(List<Vec2> polyline, double cellLeft, double cellTop, int scale, double halfWidth) {
        int inside = 0;
        for (int sx = 0; sx < SAMPLES_PER_AXIS; sx++) {
            for (int sy = 0; sy < SAMPLES_PER_AXIS; sy++) {
                double x = cellLeft + (sx + 0.5) * scale / SAMPLES_PER_AXIS;
                double y = cellTop + (sy + 0.5) * scale / SAMPLES_PER_AXIS;
                if (distanceToPolyline(polyline, x, y) <= halfWidth) {
                    inside++;
                }
            }
        }
        return (double) inside / (SAMPLES_PER_AXIS * SAMPLES_PER_AXIS);
    }

    private static double distanceToPolyline(List<Vec2> polyline, double x, double y) {
        double minDistance = Double.MAX_VALUE;
        for (int i = 0; i < polyline.size() - 1; i++) {
            minDistance = Math.min(minDistance, distanceToSegment(polyline.get(i), polyline.get(i + 1), x, y));
        }
        return minDistance;
    }

    private static double distanceToSegment(Vec2 a, Vec2 b, double x, double y) {
        double dx = b.x() - a.x();
        double dy = b.y() - a.y();
        double lengthSquared = dx * dx + dy * dy;
        double t = lengthSquared == 0.0 ? 0.0 : ((x - a.x()) * dx + (y - a.y()) * dy) / lengthSquared;
        t = Math.max(0.0, Math.min(1.0, t));
        double px = a.x() + t * dx;
        double py = a.y() + t * dy;
        return Math.hypot(x - px, y - py);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
