package td.tower.mortar;

import td.wave.ArcLengthPath;
import td.wave.Path;
import td.wave.Vec2;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

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
        return paths.stream()
                .flatMap(path -> ArcLengthPath.of(path).stream())
                .map(measured -> new Spot(measured, measured.nearest(x, y)))
                .min(Comparator.comparingDouble(spot -> spot.at().distance()))
                .map(spot -> Arrays.stream(offsets)
                        .mapToObj(offset -> spot.path().poseAt(spot.at().along() + offset).position())
                        .toList())
                .orElse(List.of());
    }

    /** The point of {@code path} nearest where the shell landed. */
    private record Spot(ArcLengthPath path, ArcLengthPath.Nearest at) {
    }
}
