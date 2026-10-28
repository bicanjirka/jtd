package td.enemy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;

class SwingMovementTest {

    @Test
    void itSwingsOutToOneSideBackThroughTheMiddleToTheOtherAndHome() {
        SwingMovement swing = new SwingMovement(1f, 20, SwingCurve.LINEAR);

        assertThat(swing.angleAt(0)).isEqualTo(0f);
        assertThat(swing.angleAt(10)).isCloseTo(0.5f, offset(1e-5f));
        assertThat(swing.angleAt(20)).isCloseTo(1f, offset(1e-5f));
        assertThat(swing.angleAt(40)).isEqualTo(0f);
        assertThat(swing.angleAt(50)).isCloseTo(-0.5f, offset(1e-5f));
        assertThat(swing.angleAt(60)).isCloseTo(-1f, offset(1e-5f));
        assertThat(swing.angleAt(80)).isEqualTo(0f);
    }

    @Test
    void anEasedSwingLagsBehindALinearOneEarlyOn() {
        SwingMovement eased = new SwingMovement(1f, 20, SwingCurve.ACCELERATING);

        assertThat(eased.angleAt(5)).isLessThan(0.25f);
        assertThat(eased.angleAt(20)).isCloseTo(1f, offset(1e-5f));
    }

    @Test
    void withoutACurveTheSwingSlowsTowardsTheExtremeAndReachesTheFullAngle() {
        SwingMovement swing = new SwingMovement(1f, 20);

        assertThat(swing.angleAt(20)).isCloseTo(1f, offset(1e-5f));
        assertThat(swing.angleAt(5)).isGreaterThan(0.25f);
    }

    @Test
    void aNegativeSwingTurnsTheOtherSideFirst() {
        assertThat(new SwingMovement(-1f, 20, SwingCurve.LINEAR).angleAt(10)).isLessThan(0f);
    }

    @Test
    void aSwingThatTakesNoTimeIsRejected() {
        assertThatThrownBy(() -> new SwingMovement(1f, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
