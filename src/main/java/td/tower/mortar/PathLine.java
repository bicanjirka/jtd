package td.tower.mortar;

import td.wave.ArcLengthPath;
import td.wave.Path;
import td.wave.Vec2;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Points along the enemies' path, measured from where a shell landed. */
public final class PathLine {

    private PathLine() {
    }

    /**
     * The points {@code offsets} pixels along the path from the point of it nearest ({@code x},
     * {@code y}), negative behind it and positive ahead of it, in the order given. Nothing when no
     * path has a length.
     */
    public static List<Vec2> along(List<Path> paths, double x, double y, double[] offsets) {
        Optional<ArcLengthPath> nearest = Optional.empty();
        double nearestDistance = Double.MAX_VALUE;
        double along = 0.0;
        for (Path path : paths) {
            Optional<ArcLengthPath> measured = ArcLengthPath.of(path);
            if (measured.isPresent()) {
                ArcLengthPath.Nearest here = measured.get().nearest(x, y);
                if (here.distance() < nearestDistance) {
                    nearestDistance = here.distance();
                    along = here.along();
                    nearest = measured;
                }
            }
        }
        List<Vec2> points = new ArrayList<>();
        if (nearest.isPresent()) {
            for (double offset : offsets) {
                points.add(nearest.get().poseAt(along + offset).position());
            }
        }
        return points;
    }
}
