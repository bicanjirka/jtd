package td.wave.smoothing;

import org.junit.jupiter.api.Test;
import td.wave.Vec2;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Uses one concrete subclass; none of these behaviours depend on the curve. */
class AbstractCornerSmoothingTest {

    @Test
    void constructorRejectsACornerPullOutsideZeroToOneHalf() {
        assertThatThrownBy(() -> new QuadraticBezierSmoothing(0, 4)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new QuadraticBezierSmoothing(-0.1, 4)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new QuadraticBezierSmoothing(0.51, 4)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructorRejectsFewerThanOneSamplePerCorner() {
        assertThatThrownBy(() -> new QuadraticBezierSmoothing(0.3, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aPathWithFewerThanThreePointsIsUnchanged() {
        List<Vec2> straight = List.of(new Vec2(0, 0), new Vec2(10, 0));

        assertThat(new QuadraticBezierSmoothing(0.3, 4).smooth(straight)).isEqualTo(straight);
    }

    @Test
    void aStraightRunOfCollinearPointsPassesThroughUnchanged() {
        List<Vec2> straight = List.of(new Vec2(0, 0), new Vec2(10, 0), new Vec2(20, 0), new Vec2(30, 0));

        assertThat(new QuadraticBezierSmoothing(0.3, 4).smooth(straight)).isEqualTo(straight);
    }

    @Test
    void aNearTotalReversalCornerIsLeftUnrounded() {
        // (10,0) -> (0,0) -> (10,0.1): almost doubles back on itself, well past the
        // near-180-degree cutoff this template refuses to round
        List<Vec2> path = List.of(new Vec2(10, 0), new Vec2(0, 0), new Vec2(10, 0.1));

        List<Vec2> smoothed = new QuadraticBezierSmoothing(0.4, 4).smooth(path);

        assertThat(smoothed).contains(new Vec2(0, 0));
    }

    @Test
    void pullbackNeverExceedsHalfOfTheShorterAdjacentLeg() {
        // corner at (10,0): legs of length 10 (from (0,0)) and 4 (to (10,4)) - the shorter leg
        // bounds the pullback on BOTH sides of the corner
        List<Vec2> path = List.of(new Vec2(0, 0), new Vec2(10, 0), new Vec2(10, 4));

        List<Vec2> smoothed = new QuadraticBezierSmoothing(0.5, 4).smooth(path);

        Vec2 pulledBackBefore = smoothed.get(1);
        Vec2 pulledBackAfter = smoothed.get(smoothed.size() - 2);
        assertThat(pulledBackBefore.x()).isCloseTo(8.0, org.assertj.core.data.Offset.offset(1e-9)); // 10 - 0.5*4
        assertThat(pulledBackAfter.y()).isCloseTo(2.0, org.assertj.core.data.Offset.offset(1e-9)); // 0 + 0.5*4
    }

    @Test
    void twoTightConsecutiveCornersPullBackWithoutCrossing() {
        // a short middle leg (length 4) shared by two corners, both at maximum cornerPull -
        // each corner's pullback consumes exactly half of it, meeting but not crossing.
        // One sample per corner keeps the resulting list's indices simple to reason about:
        // [0]=(0,0) [1]=pulledBackBefore1 [2]=bezier sample [3]=pulledBackAfter1
        // [4]=pulledBackBefore2 [5]=bezier sample [6]=pulledBackAfter2 [7]=(0,4)
        List<Vec2> path = List.of(new Vec2(0, 0), new Vec2(10, 0), new Vec2(10, 4), new Vec2(0, 4));

        List<Vec2> smoothed = new QuadraticBezierSmoothing(0.5, 1).smooth(path);

        assertThat(smoothed).hasSize(8);
        double firstCornerPullbackAfterY = smoothed.get(3).y();
        double secondCornerPullbackBeforeY = smoothed.get(4).y();
        assertThat(firstCornerPullbackAfterY).isLessThanOrEqualTo(secondCornerPullbackBeforeY + 1e-9);
        assertThat(smoothed.get(3)).isEqualTo(smoothed.get(4)); // touching exactly, not crossing
    }
}
