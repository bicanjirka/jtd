package td.wave;

import java.util.List;
import java.util.Optional;

/**
 * Wraps a {@link Path}'s points with the cumulative distance travelled to reach each one, so
 * any distance travelled along the path resolves to an exact position and facing by linear
 * interpolation within the segment it falls in - diagonal or curved segments included, nothing
 * here assumes axis-aligned or uniform-length steps. Shared by enemy movement and the animated
 * path-marker overlay, which both need "where is distance d along this path" and previously
 * each had their own copy of this math.
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

    /**
     * Empty for a path with fewer than two points, or zero total length.
     */
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
        // Clamp rather than extrapolate: the original code this was ported from only ever
        // called poseAt with a distance already wrapped into [0, totalLength) - but as a
        // shared, more widely-used type, an out-of-range distance should clamp to the
        // nearest endpoint rather than extrapolate past it.
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
