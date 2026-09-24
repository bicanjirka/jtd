package td;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TickAccumulatorTest {

    private static final long STEP = 50_000_000L; // 50ms

    @Test
    void zeroElapsedProducesNoTicks() {
        TickAccumulator accumulator = new TickAccumulator(STEP);

        assertThat(accumulator.accumulate(0)).isZero();
    }

    @Test
    void exactMultipleOfStepProducesThatManyTicksWithNoRemainder() {
        TickAccumulator accumulator = new TickAccumulator(STEP);

        assertThat(accumulator.accumulate(STEP * 3)).isEqualTo(3);
        assertThat(accumulator.accumulate(STEP / 2)).isZero();
    }

    @Test
    void subStepElapsedCarriesOverAcrossCalls() {
        TickAccumulator accumulator = new TickAccumulator(STEP);

        assertThat(accumulator.accumulate(STEP / 2)).isZero();
        assertThat(accumulator.accumulate(STEP / 2)).isEqualTo(1);
    }

    @Test
    void largeStallProducesManyTicksInOneCall() {
        TickAccumulator accumulator = new TickAccumulator(STEP);

        assertThat(accumulator.accumulate(STEP * 200)).isEqualTo(200);
    }

    @Test
    void reflectsTickSpeedPresetsWhenElapsedIsPreScaledByTheMultiplier() {
        TickAccumulator accumulator = new TickAccumulator(STEP);
        long realElapsed = STEP; // one baseline tick's worth of real time

        assertThat(accumulator.accumulate((long) (realElapsed * TickSpeed.PAUSED.multiplier()))).isZero();
        assertThat(accumulator.accumulate((long) (realElapsed * TickSpeed.NORMAL.multiplier()))).isEqualTo(1);
        assertThat(accumulator.accumulate((long) (realElapsed * TickSpeed.FAST.multiplier()))).isEqualTo(3);
        assertThat(accumulator.accumulate((long) (realElapsed * TickSpeed.SUPER_FAST.multiplier()))).isEqualTo(16);
    }

    @Test
    void rejectsNonPositiveStep() {
        assertThatThrownBy(() -> new TickAccumulator(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TickAccumulator(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativeElapsed() {
        TickAccumulator accumulator = new TickAccumulator(STEP);

        assertThatThrownBy(() -> accumulator.accumulate(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void resetDiscardsAnyCarriedRemainder() {
        TickAccumulator accumulator = new TickAccumulator(STEP);
        accumulator.accumulate(STEP / 2);

        accumulator.reset();

        assertThat(accumulator.accumulate(STEP / 2)).isZero();
    }

    @Test
    void fractionElapsedReflectsTheCarriedSubStepRemainder() {
        TickAccumulator accumulator = new TickAccumulator(STEP);

        assertThat(accumulator.fractionElapsed()).isZero();

        accumulator.accumulate(STEP / 4);
        assertThat(accumulator.fractionElapsed()).isEqualTo(0.25);

        accumulator.accumulate(STEP / 2); // now 3/4 of a step carried
        assertThat(accumulator.fractionElapsed()).isEqualTo(0.75);

        accumulator.accumulate(STEP / 2); // crosses a whole tick, only the remainder carries
        assertThat(accumulator.fractionElapsed()).isEqualTo(0.25);
    }

    @Test
    void fractionElapsedIsZeroAfterReset() {
        TickAccumulator accumulator = new TickAccumulator(STEP);
        accumulator.accumulate(STEP / 2);

        accumulator.reset();

        assertThat(accumulator.fractionElapsed()).isZero();
    }
}
