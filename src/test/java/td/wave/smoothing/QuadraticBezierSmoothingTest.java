package td.wave.smoothing;

import org.junit.jupiter.api.Test;
import td.wave.Vec2;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class QuadraticBezierSmoothingTest {

    @Test
    void theMidpointSampleMatchesTheQuadraticBezierFormula() {
        // B(0.5) = 0.25*before + 0.5*corner + 0.25*after
        Vec2 before = new Vec2(0, 0);
        Vec2 corner = new Vec2(10, 0);
        Vec2 after = new Vec2(10, 10);

        List<Vec2> samples = new QuadraticBezierSmoothing(0.5, 1).sampleCorner(before, corner, after, 1);

        assertThat(samples).hasSize(1);
        assertThat(samples.get(0).x()).isCloseTo(7.5, within(1e-9));
        assertThat(samples.get(0).y()).isCloseTo(2.5, within(1e-9));
    }

    @Test
    void theMidpointSampleMatchesTheQuadraticBezierFormulaForANonRightAngleCorner() {
        // same 6-8-10 triangle corner as ArcCornerSmoothingTest's non-right-angle case (about
        // 53 degrees, neither 90 nor 45) - the bezier formula is angle-agnostic by construction,
        // but this proves it directly rather than only for a 90 degree corner.
        Vec2 before = new Vec2(5, 0);
        Vec2 corner = new Vec2(10, 0);
        Vec2 after = new Vec2(13, 4);

        List<Vec2> samples = new QuadraticBezierSmoothing(0.5, 1).sampleCorner(before, corner, after, 1);

        assertThat(samples).hasSize(1);
        assertThat(samples.get(0).x()).isCloseTo(9.5, within(1e-9));
        assertThat(samples.get(0).y()).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void endToEndSmoothingPullsBackTheCornerBeforeCurving() {
        List<Vec2> path = List.of(new Vec2(0, 0), new Vec2(10, 0), new Vec2(10, 10));

        List<Vec2> smoothed = new QuadraticBezierSmoothing(0.3, 4).smooth(path);

        assertThat(smoothed.get(0)).isEqualTo(new Vec2(0, 0));
        assertThat(smoothed.get(1)).isEqualTo(new Vec2(7, 0)); // pulledBackBefore: 10 - 0.3*10
        assertThat(smoothed.get(smoothed.size() - 2)).isEqualTo(new Vec2(10, 3)); // pulledBackAfter: 0 + 0.3*10
        assertThat(smoothed.get(smoothed.size() - 1)).isEqualTo(new Vec2(10, 10));
        assertThat(smoothed).hasSize(1 + 1 + 4 + 1 + 1); // start, pullback-before, 4 samples, pullback-after, end
    }
}
