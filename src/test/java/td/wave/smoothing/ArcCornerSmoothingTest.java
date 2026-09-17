package td.wave.smoothing;

import org.junit.jupiter.api.Test;
import td.wave.Vec2;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ArcCornerSmoothingTest {

    @Test
    void a90DegreeCornersArcHasRadiusEqualToThePullbackDistanceAndIsCenteredDiagonally() {
        // corner (10,0), legs of length 10 each - pullback at cornerPull=0.5 is 5, giving
        // pulledBackBefore=(5,0), pulledBackAfter=(10,5). For a 90 degree corner the tangent
        // circle's center sits at the diagonal point (5,5), radius exactly 5 - a square's
        // corner, by the symmetry of two perpendicular tangent lines.
        Vec2 before = new Vec2(5, 0);
        Vec2 corner = new Vec2(10, 0);
        Vec2 after = new Vec2(10, 5);

        List<Vec2> samples = new ArcCornerSmoothing(0.5, 8).sampleCorner(before, corner, after, 8);

        for (Vec2 p : samples) {
            double distanceFromCenter = Math.hypot(p.x() - 5, p.y() - 5);
            assertThat(distanceFromCenter).isCloseTo(5.0, within(1e-9));
        }
    }

    @Test
    void aNonRightAngleCornersArcIsStillCenteredOnTheTangentCircleIntersection() {
        // pulledBackBefore=(5,0), corner=(10,0), pulledBackAfter=(13,4) - a 6-8-10 triangle
        // corner turning about 53 degrees, neither 90 nor 45, proving the tangent-circle
        // construction was never angle-specific. Solving the two tangent-line perpendiculars
        // independently of the production code gives center (5,10), radius 10.
        Vec2 before = new Vec2(5, 0);
        Vec2 corner = new Vec2(10, 0);
        Vec2 after = new Vec2(13, 4);

        List<Vec2> samples = new ArcCornerSmoothing(0.5, 8).sampleCorner(before, corner, after, 8);

        assertThat(samples).hasSize(8);
        for (Vec2 p : samples) {
            double distanceFromCenter = Math.hypot(p.x() - 5, p.y() - 10);
            assertThat(distanceFromCenter).isCloseTo(10.0, within(1e-9));
        }
    }

    @Test
    void theArcBulgesTowardTheCornerRatherThanAwayFromIt() {
        Vec2 before = new Vec2(5, 0);
        Vec2 corner = new Vec2(10, 0);
        Vec2 after = new Vec2(10, 5);

        List<Vec2> samples = new ArcCornerSmoothing(0.5, 1).sampleCorner(before, corner, after, 1);

        // the single midpoint sample should land close to the corner's own quadrant (large x,
        // small y), not swing to the opposite side of the circle (small x, large y)
        Vec2 midpoint = samples.getFirst();
        assertThat(midpoint.x()).isGreaterThan(5.0);
        assertThat(midpoint.y()).isLessThan(5.0);
    }

    @Test
    void endToEndSmoothingReplacesA90DegreeCornerWithAnArcTangentToBothLegs() {
        List<Vec2> path = List.of(new Vec2(0, 0), new Vec2(10, 0), new Vec2(10, 10));

        List<Vec2> smoothed = new ArcCornerSmoothing(0.5, 4).smooth(path);

        assertThat(smoothed.get(0)).isEqualTo(new Vec2(0, 0));
        assertThat(smoothed.get(1)).isEqualTo(new Vec2(5, 0)); // pulledBackBefore
        assertThat(smoothed.get(smoothed.size() - 2)).isEqualTo(new Vec2(10, 5)); // pulledBackAfter
        assertThat(smoothed.getLast()).isEqualTo(new Vec2(10, 10));
    }
}
