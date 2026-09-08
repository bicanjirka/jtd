package td.wave.smoothing;

import td.wave.Vec2;

import java.util.ArrayList;
import java.util.List;

/**
 * Rounds each corner with a quadratic Bezier curve, using the corner itself as the single
 * control point: {@code B(t) = (1-t)^2 * before + 2(1-t)t * corner + t^2 * after}. A cheaper,
 * simpler alternative to {@link ArcCornerSmoothing}'s exact circular arc - it doesn't stay
 * exactly tangent to both legs, but bulges toward the corner in a visually similar way.
 */
public final class QuadraticBezierSmoothing extends AbstractCornerSmoothing {

    public QuadraticBezierSmoothing(double cornerPull, int samplesPerCorner) {
        super(cornerPull, samplesPerCorner);
    }

    @Override
    protected List<Vec2> sampleCorner(Vec2 pulledBackBefore, Vec2 corner, Vec2 pulledBackAfter, int samples) {
        List<Vec2> points = new ArrayList<>(samples);
        for (int i = 1; i <= samples; i++) {
            double t = (double) i / (samples + 1);
            double oneMinusT = 1 - t;
            double a = oneMinusT * oneMinusT;
            double b = 2 * oneMinusT * t;
            double c = t * t;
            double x = a * pulledBackBefore.x() + b * corner.x() + c * pulledBackAfter.x();
            double y = a * pulledBackBefore.y() + b * corner.y() + c * pulledBackAfter.y();
            points.add(new Vec2(x, y));
        }
        return points;
    }
}
