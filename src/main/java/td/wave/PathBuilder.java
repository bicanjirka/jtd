package td.wave;

import td.wave.smoothing.PathSmoothing;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a {@link Path} from a level's raw cell-coordinate path: converts each cell to its
 * pixel-space center, runs the result through the level's {@link PathSmoothing} strategy,
 * then populates a {@link PathNormal}.
 */
public final class PathBuilder {

    private PathBuilder() {
    }

    public static Path build(List<Point> cellPath, PathSmoothing smoothing, int scale) {
        List<Vec2> pixelCenters = new ArrayList<>(cellPath.size());
        for (Point cell : cellPath) {
            pixelCenters.add(new Vec2(cell.x() * scale + (scale / 2), cell.y() * scale + (scale / 2)));
        }
        List<Vec2> smoothed = smoothing.smooth(pixelCenters);

        PathNormal path = new PathNormal(scale);
        for (Vec2 point : smoothed) {
            path.addStep(point.x(), point.y());
        }
        return path;
    }
}
