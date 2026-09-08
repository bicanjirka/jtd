package td.level;

import td.wave.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * Expands a hand-authored list of path corners into the cell-by-cell list a
 * {@link LevelDefinition} needs. Corners alone (a much smaller, easier to author list) are not
 * enough - every cell in between has to be listed too, since this is the list
 * {@link td.wave.PathBuilder} turns into real pixel geometry: {@link td.wave.PathCoverage}
 * determines buildability from that geometry (not from a sparser corner list), and a
 * smoothing strategy (see {@code td.wave.smoothing}) reshapes this same per-cell polyline into
 * a curve.
 */
public final class LevelPath {

    private LevelPath() {
    }

    public static List<Point> throughCorners(Point... corners) {
        List<Point> steps = new ArrayList<>();
        if (corners.length == 0) {
            return steps;
        }
        steps.add(corners[0]);
        for (int i = 1; i < corners.length; i++) {
            Point from = corners[i - 1];
            Point to = corners[i];
            if (from.x() != to.x() && from.y() != to.y()) {
                throw new IllegalArgumentException(
                        "Path corners must be axis-aligned, but " + from + " -> " + to + " is diagonal");
            }
            int stepX = Integer.signum(to.x() - from.x());
            int stepY = Integer.signum(to.y() - from.y());
            int x = from.x();
            int y = from.y();
            while (x != to.x() || y != to.y()) {
                x += stepX;
                y += stepY;
                steps.add(new Point(x, y));
            }
        }
        return steps;
    }
}
