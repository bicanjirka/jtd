package td.level;

import td.wave.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * Expands a hand-authored list of path corners into the cell-by-cell list a
 * {@link LevelDefinition} needs: {@link td.wave.PathNormal#finalise} marks
 * every listed cell unbuildable, and enemy movement advances one path step
 * per tick, so corners alone (a much smaller, easier to author list) are not
 * enough - every cell in between has to be listed too.
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
