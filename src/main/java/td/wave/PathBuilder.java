package td.wave;

import td.wave.smoothing.PathSmoothing;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a {@link Path} from corner cells: to pixel centres, then through the level's
 * {@link PathSmoothing}.
 */
public final class PathBuilder {

    private PathBuilder() {
    }

    public static Path build(List<Point> corners, PathSmoothing smoothing, int scale) {
        List<Vec2> pixelCenters = new ArrayList<>(corners.size());
        for (Point cell : corners) {
            pixelCenters.add(new Vec2(cell.x() * scale + (scale / 2), cell.y() * scale + (scale / 2)));
        }
        return new PathNormal(smoothing.smooth(pixelCenters));
    }
}
