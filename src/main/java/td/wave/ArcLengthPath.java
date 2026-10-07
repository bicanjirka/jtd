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

    /**
     * Whether the stretch of path between distances {@code from} and {@code to} comes within
     * {@code radius} of the point. Allocation-free: it runs once per tower for every finished walk.
     */
    public boolean passesWithin(double pointX, double pointY, double radius, double from, double to) {
        double start = Math.max(0.0, Math.min(from, to));
        double end = Math.min(this.totalLength, Math.max(from, to));
        double radius2 = radius * radius;
        for (int i = 0; i < this.cumulative.length - 1; i++) {
            double segmentStart = this.cumulative[i];
            double segmentEnd = this.cumulative[i + 1];
            if (segmentEnd < start || segmentStart > end || segmentEnd == segmentStart) {
                continue;
            }
            double length = segmentEnd - segmentStart;
            double t0 = (Math.max(start, segmentStart) - segmentStart) / length;
            double t1 = (Math.min(end, segmentEnd) - segmentStart) / length;
            double ax = lerp(this.xs[i], this.xs[i + 1], t0);
            double ay = lerp(this.ys[i], this.ys[i + 1], t0);
            double bx = lerp(this.xs[i], this.xs[i + 1], t1);
            double by = lerp(this.ys[i], this.ys[i + 1], t1);
            if (distanceToSegment2(pointX, pointY, ax, ay, bx, by) <= radius2) {
                return true;
            }
        }
        return false;
    }

    private static double distanceToSegment2(double px, double py, double ax, double ay, double bx, double by) {
        double dx = bx - ax;
        double dy = by - ay;
        double length2 = dx * dx + dy * dy;
        double t = length2 == 0.0 ? 0.0 : Math.max(0.0, Math.min(1.0, ((px - ax) * dx + (py - ay) * dy) / length2));
        double cx = ax + t * dx - px;
        double cy = ay + t * dy - py;
        return cx * cx + cy * cy;
    }

    /**
     * The point on the path closest to ({@code pointX}, {@code pointY}): how far along the path it is,
     * and how far from the point.
     */
    public Nearest nearest(double pointX, double pointY) {
        double bestAlong = 0.0;
        double bestDistance2 = Double.MAX_VALUE;
        for (int i = 0; i < this.cumulative.length - 1; i++) {
            double ax = this.xs[i];
            double ay = this.ys[i];
            double dx = this.xs[i + 1] - ax;
            double dy = this.ys[i + 1] - ay;
            double length2 = dx * dx + dy * dy;
            double t = length2 == 0.0 ? 0.0 : Math.max(0.0, Math.min(1.0, ((pointX - ax) * dx + (pointY - ay) * dy) / length2));
            double cx = ax + t * dx - pointX;
            double cy = ay + t * dy - pointY;
            double distance2 = cx * cx + cy * cy;
            if (distance2 < bestDistance2) {
                bestDistance2 = distance2;
                bestAlong = this.cumulative[i] + t * (this.cumulative[i + 1] - this.cumulative[i]);
            }
        }
        return new Nearest(bestAlong, Math.sqrt(bestDistance2));
    }

    /** The closest point of a path to somewhere: its distance along the path, and from that somewhere. */
    public record Nearest(double along, double distance) {
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
