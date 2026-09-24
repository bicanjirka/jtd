package td.wave;

import java.util.List;
import java.util.Optional;

/**
 * A {@link Path} with the cumulative distance to each point, so any distance along it resolves to
 * an exact position and facing, whatever the segment shapes. Shared by enemy movement and path
 * markers.
 */
public final class ArcLengthPath {

    private final double[] xs;
    private final double[] ys;
    private final double[] cumulative;
    private final double totalLength;

    private ArcLengthPath(double[] xs, double[] ys, double[] cumulative, double totalLength) {
        this.xs = xs;
        this.ys = ys;
        this.cumulative = cumulative;
        this.totalLength = totalLength;
    }

    /** Empty for fewer than two points or zero length. */
    public static Optional<ArcLengthPath> of(Path path) {
        List<Vec2> points = path.points();
        int n = points.size();
        if (n < 2) {
            return Optional.empty();
        }
        double[] xs = new double[n];
        double[] ys = new double[n];
        for (int i = 0; i < n; i++) {
            Vec2 p = points.get(i);
            xs[i] = p.x();
            ys[i] = p.y();
        }
        double[] cumulative = new double[n];
        for (int i = 1; i < n; i++) {
            cumulative[i] = cumulative[i - 1] + Math.hypot(xs[i] - xs[i - 1], ys[i] - ys[i - 1]);
        }
        double totalLength = cumulative[n - 1];
        if (totalLength <= 0.0) {
            return Optional.empty();
        }
        return Optional.of(new ArcLengthPath(xs, ys, cumulative, totalLength));
    }

    private static double lerp(double from, double to, double t) {
        return from + (to - from) * t;
    }

    public double totalLength() {
        return this.totalLength;
    }

    public PathPose poseAt(double distance) {
        distance = Math.max(0.0, Math.min(distance, this.totalLength));
        int segment = this.cumulative.length - 2;
        for (int i = 0; i < this.cumulative.length - 1; i++) {
            if (distance < this.cumulative[i + 1]) {
                segment = i;
                break;
            }
        }
        double segmentLength = this.cumulative[segment + 1] - this.cumulative[segment];
        double t = segmentLength == 0.0 ? 0.0 : (distance - this.cumulative[segment]) / segmentLength;
        double x = lerp(this.xs[segment], this.xs[segment + 1], t);
        double y = lerp(this.ys[segment], this.ys[segment + 1], t);
        double facing = Math.atan2(this.ys[segment + 1] - this.ys[segment], this.xs[segment + 1] - this.xs[segment]);
        return new PathPose(new Vec2(x, y), facing);
    }
}
