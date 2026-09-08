package td.wave.smoothing;

import td.wave.Vec2;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared corner-replacement skeleton for a family of {@link PathSmoothing} strategies: walk
 * each interior corner of the raw polyline, pull back a fraction of its shorter adjacent leg
 * on each side, and delegate the corner itself to a subclass-specific curve. A degenerate
 * corner - already straight, or so sharp it's a near-total reversal a tangent-circle/curve
 * construction can't handle cleanly - is passed through unrounded rather than forced into one.
 */
public abstract class AbstractCornerSmoothing implements PathSmoothing {

    private static final double COLLINEAR_ANGLE_RADIANS = 1e-6;
    // A turn sharper than this (nearly a dead-end U-turn) is left unrounded, since a
    // tangent-circle/curve construction can't handle a near-total reversal cleanly.
    private static final double MIN_TURN_ANGLE_RADIANS = Math.toRadians(10);

    private final double cornerPull;
    private final int samplesPerCorner;

    /**
     * @param cornerPull       fraction, in (0, 0.5], of the shorter adjacent leg's length to
     *                         pull back from the corner before replacing it with a curve. A
     *                         fraction rather than a fixed pixel distance so it scales down
     *                         automatically on tightly-spaced corners instead of overlapping
     *                         a neighboring corner's own rounding.
     * @param samplesPerCorner how many points to sample along each corner's curve
     */
    protected AbstractCornerSmoothing(double cornerPull, int samplesPerCorner) {
        if (cornerPull <= 0 || cornerPull > 0.5) {
            throw new IllegalArgumentException("cornerPull must be in (0, 0.5], was " + cornerPull);
        }
        if (samplesPerCorner < 1) {
            throw new IllegalArgumentException("samplesPerCorner must be at least 1, was " + samplesPerCorner);
        }
        this.cornerPull = cornerPull;
        this.samplesPerCorner = samplesPerCorner;
    }

    /**
     * Samples one corner's replacement curve, given the two points it has been pulled back to
     * (equidistant from the corner along each adjacent leg, by construction). Returns strictly
     * interior points only, in order from {@code pulledBackBefore} to {@code pulledBackAfter}
     * - the template adds the pullback points themselves.
     */
    protected abstract List<Vec2> sampleCorner(Vec2 pulledBackBefore, Vec2 corner, Vec2 pulledBackAfter, int samples);

    @Override
    public final List<Vec2> smooth(List<Vec2> pixelPolyline) {
        if (pixelPolyline.size() < 3) {
            return List.copyOf(pixelPolyline);
        }
        List<Vec2> result = new ArrayList<>();
        result.add(pixelPolyline.get(0));

        for (int i = 1; i < pixelPolyline.size() - 1; i++) {
            Vec2 prev = pixelPolyline.get(i - 1);
            Vec2 corner = pixelPolyline.get(i);
            Vec2 next = pixelPolyline.get(i + 1);

            if (isDegenerateCorner(prev, corner, next)) {
                result.add(corner);
                continue;
            }

            double pullback = this.cornerPull * Math.min(distance(prev, corner), distance(corner, next));
            Vec2 pulledBackBefore = pointToward(corner, prev, pullback);
            Vec2 pulledBackAfter = pointToward(corner, next, pullback);

            result.add(pulledBackBefore);
            result.addAll(sampleCorner(pulledBackBefore, corner, pulledBackAfter, this.samplesPerCorner));
            result.add(pulledBackAfter);
        }

        result.add(pixelPolyline.get(pixelPolyline.size() - 1));
        return result;
    }

    private static boolean isDegenerateCorner(Vec2 prev, Vec2 corner, Vec2 next) {
        double v1x = corner.x() - prev.x();
        double v1y = corner.y() - prev.y();
        double v2x = next.x() - corner.x();
        double v2y = next.y() - corner.y();
        double cross = v1x * v2y - v1y * v2x;
        double dot = v1x * v2x + v1y * v2y;
        double turnAngle = Math.atan2(cross, dot);
        return Math.abs(turnAngle) < COLLINEAR_ANGLE_RADIANS || Math.abs(turnAngle) > Math.PI - MIN_TURN_ANGLE_RADIANS;
    }

    private static double distance(Vec2 a, Vec2 b) {
        return Math.hypot(b.x() - a.x(), b.y() - a.y());
    }

    private static Vec2 pointToward(Vec2 from, Vec2 towards, double distance) {
        double dx = towards.x() - from.x();
        double dy = towards.y() - from.y();
        double t = distance / Math.hypot(dx, dy);
        return new Vec2(from.x() + dx * t, from.y() + dy * t);
    }
}
