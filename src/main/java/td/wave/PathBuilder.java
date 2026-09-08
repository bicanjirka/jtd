package td.wave;

import java.util.List;

/**
 * Builds a {@link Path} from a level's raw cell-coordinate path: converts each cell to its
 * pixel-space center, then populates a {@link PathNormal}. Path smoothing lands here in a
 * later phase, between the conversion and the population.
 */
public final class PathBuilder {

    private PathBuilder() {
    }

    public static Path build(List<Point> cellPath, int scale) {
        PathNormal path = new PathNormal(scale);
        for (Point cell : cellPath) {
            path.addStep(cell.x() * scale + (scale / 2), cell.y() * scale + (scale / 2));
        }
        return path;
    }
}
