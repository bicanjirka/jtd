package td.tower.sonar;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class SonarSweepTest {

    private static final double TWO_PI = Math.PI * 2;
    private static final double TICKS_PER_SECOND = 20;

    /** Two seconds at 20 ticks a second: 40 ticks per revolution. */
    private static SonarSweep twoSecondSweep() {
        return SonarSweep.perRevolution(2, TICKS_PER_SECOND);
    }

    @Test
    void oneRevolutionTakesTheConfiguredNumberOfSeconds() {
        SonarSweep sweep = twoSecondSweep();

        assertThat(sweep.radiansPerTick()).isCloseTo(TWO_PI / 40, within(1e-12));
    }

    @Test
    void aBearingIsCrossedExactlyOncePerRevolution() {
        SonarSweep sweep = twoSecondSweep();
        double bearing = 1.0;
        int crossings = 0;

        for (int tick = 0; tick < 40; tick++) {
            sweep.advance();
            if (sweep.sweptThisTick(bearing)) {
                crossings++;
            }
        }

        assertThat(crossings).isEqualTo(1);
    }

    @Test
    void everyBearingOnTheCircleIsCrossedWithinOneRevolution() {
        SonarSweep sweep = twoSecondSweep();
        int bearingCount = 360;
        boolean[] crossed = new boolean[bearingCount];

        for (int tick = 0; tick < 40; tick++) {
            sweep.advance();
            for (int i = 0; i < bearingCount; i++) {
                if (sweep.sweptThisTick(-Math.PI + i * TWO_PI / bearingCount)) {
                    crossed[i] = true;
                }
            }
        }

        assertThat(crossed).containsOnly(true);
    }

    @Test
    void aBearingJustPassedIsNotCrossedAgainOnTheNextTick() {
        SonarSweep sweep = twoSecondSweep();
        sweep.advance();
        // the angle the beam sat on at the start of the tick it just took
        double justPassed = sweep.radiansAt(0);
        assertThat(sweep.sweptThisTick(justPassed)).isTrue();

        sweep.advance();

        assertThat(sweep.sweptThisTick(justPassed)).isFalse();
    }

    @Test
    void theBeamTurnsCounterclockwiseWhichMeansADecreasingAngle() {
        SonarSweep sweep = twoSecondSweep();

        sweep.advance();
        double start = sweep.radiansAt(0);
        double end = sweep.radiansAt(1);

        assertThat(end).isLessThan(start);
    }

    @Test
    void interpolatingAcrossATickWalksFromTheStartAngleToTheEndAngle() {
        SonarSweep sweep = twoSecondSweep();
        sweep.advance();

        double start = sweep.radiansAt(0);
        double half = sweep.radiansAt(0.5);
        double end = sweep.radiansAt(1);

        assertThat(start - half).isCloseTo(sweep.radiansPerTick() / 2, within(1e-12));
        assertThat(start - end).isCloseTo(sweep.radiansPerTick(), within(1e-12));
    }

    @Test
    void crossingStillWorksAfterTheAngleHasWrappedAroundManyTimes() {
        SonarSweep sweep = twoSecondSweep();
        double bearing = 2.5;
        int crossings = 0;

        for (int tick = 0; tick < 40 * 7; tick++) {
            sweep.advance();
            if (sweep.sweptThisTick(bearing)) {
                crossings++;
            }
        }

        assertThat(crossings).isEqualTo(7);
    }

    @Test
    void aNonPositiveRateIsRejected() {
        assertThatThrownBy(() -> SonarSweep.perRevolution(0, TICKS_PER_SECOND))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SonarSweep.perRevolution(2, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aRevolutionCompletesOnItsLastTickAndOnlyThen() {
        SonarSweep sweep = SonarSweep.perRevolution(2, 20);
        int completions = 0;
        int lastCompletedAt = 0;

        for (int tick = 1; tick <= 80; tick++) {
            sweep.advance();
            if (sweep.completedRevolution()) {
                completions++;
                lastCompletedAt = tick;
            }
        }

        assertThat(completions).isEqualTo(2);
        assertThat(lastCompletedAt).isEqualTo(80);
    }

    @Test
    void aRetimedSweepCarriesOnFromTheSameHeadingAtItsNewSpeed() {
        SonarSweep slow = SonarSweep.perRevolution(3, 20);
        for (int tick = 0; tick < 15; tick++) {
            slow.advance();
        }

        double heading = slow.radiansAt(1);

        SonarSweep fast = slow.retimed(2, 20);
        fast.advance();

        assertThat(fast.radiansAt(0)).isCloseTo(heading, within(1e-9));
        assertThat(fast.radiansAt(0) - fast.radiansAt(1)).isCloseTo(TWO_PI / 40, within(1e-12));
    }
}
