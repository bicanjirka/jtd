package td.wave.smoothing;

import td.wave.Vec2;

import java.util.ArrayList;
import java.util.List;

/**
 * Rounds each corner with a circular arc tangent to both adjacent legs at the pullback
 * points. Since those two points are, by construction, equidistant from the corner, the
 * arc's center lies on the intersection of the perpendiculars erected at each of them - the
 * standard tangent-circle fillet construction.
 */
public final class ArcCornerSmoothing extends AbstractCornerSmoothing {

    public ArcCornerSmoothing(double cornerPull, int samplesPerCorner) {
        super(cornerPull, samplesPerCorner);
    }

    @Override
    protected List<Vec2> sampleCorner(Vec2 pulledBackBefore, Vec2 corner, Vec2 pulledBackAfter, int samples) {
        Vec2 center = arcCenter(pulledBackBefore, corner, pulledBackAfter);
        double radius = Math.hypot(pulledBackBefore.x() - center.x(), pulledBackBefore.y() - center.y());
        double startAngle = Math.atan2(pulledBackBefore.y() - center.y(), pulledBackBefore.x() - center.x());
        double endAngle = Math.atan2(pulledBackAfter.y() - center.y(), pulledBackAfter.x() - center.x());
        double sweep = shortestSignedAngle(endAngle - startAngle);

        List<Vec2> points = new ArrayList<>(samples);
        for (int i = 1; i <= samples; i++) {
            double angle = startAngle + sweep * ((double) i / (samples + 1));
            points.add(new Vec2(center.x() + radius * Math.cos(angle), center.y() + radius * Math.sin(angle)));
        }
        return points;
    }

    /**
     * The tangent-circle center: {@code pulledBackBefore} and {@code pulledBackAfter} both
     * lie on the circle, and the circle is tangent to each leg at its respective point, so the
     * center lies on the perpendicular to that leg erected at that point - solved as the
     * intersection of the two perpendiculars.
     */
    private static Vec2 arcCenter(Vec2 pulledBackBefore, Vec2 corner, Vec2 pulledBackAfter) {
        double leg1Length = Math.hypot(corner.x() - pulledBackBefore.x(), corner.y() - pulledBackBefore.y());
        double leg2Length = Math.hypot(pulledBackAfter.x() - corner.x(), pulledBackAfter.y() - corner.y());
        double dir1x = (corner.x() - pulledBackBefore.x()) / leg1Length;
        double dir1y = (corner.y() - pulledBackBefore.y()) / leg1Length;
        double dir2x = (pulledBackAfter.x() - corner.x()) / leg2Length;
        double dir2y = (pulledBackAfter.y() - corner.y()) / leg2Length;
        double perp1x = -dir1y;
        double perp1y = dir1x;
        double perp2x = -dir2y;
        double perp2y = dir2x;

        // Solve pulledBackBefore + t1*perp1 == pulledBackAfter + t2*perp2 for t1.
        double rhsX = pulledBackAfter.x() - pulledBackBefore.x();
        double rhsY = pulledBackAfter.y() - pulledBackBefore.y();
        double a11 = perp1x, a12 = -perp2x;
        double a21 = perp1y, a22 = -perp2y;
        double det = a11 * a22 - a12 * a21;
        double t1 = (rhsX * a22 - a12 * rhsY) / det;

        return new Vec2(pulledBackBefore.x() + t1 * perp1x, pulledBackBefore.y() + t1 * perp1y);
    }

    /** Normalizes an angular difference into (-pi, pi] - the shorter way around the circle. */
    private static double shortestSignedAngle(double angle) {
        double normalized = angle % (2 * Math.PI);
        if (normalized > Math.PI) {
            normalized -= 2 * Math.PI;
        } else if (normalized <= -Math.PI) {
            normalized += 2 * Math.PI;
        }
        return normalized;
    }
}
