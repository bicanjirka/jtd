package td;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TickSpeedTest {

    @Test
    void multipliersPreserveTheOriginalTickTimeRatios() {
        assertThat(TickSpeed.PAUSED.multiplier()).isZero();
        assertThat(TickSpeed.NORMAL.multiplier()).isEqualTo(1.0);
        assertThat(TickSpeed.FAST.multiplier()).isEqualTo(50.0 / 15.0);
        assertThat(TickSpeed.SUPER_FAST.multiplier()).isEqualTo(50.0 / 3.0);
    }

    @Test
    void nextCyclesThroughThePlayablePresets() {
        assertThat(TickSpeed.NORMAL.next()).isEqualTo(TickSpeed.FAST);
        assertThat(TickSpeed.FAST.next()).isEqualTo(TickSpeed.SUPER_FAST);
        assertThat(TickSpeed.SUPER_FAST.next()).isEqualTo(TickSpeed.NORMAL);
    }

    @Test
    void nextFromPausedGoesToNormal() {
        assertThat(TickSpeed.PAUSED.next()).isEqualTo(TickSpeed.NORMAL);
    }

    @Test
    void nextFromASpeedBetweenPresetsGoesToTheNextFasterPreset() {
        assertThat(TickSpeed.ofTicksPerSecond(30).next()).isEqualTo(TickSpeed.FAST);
        assertThat(TickSpeed.ofTicksPerSecond(500).next()).isEqualTo(TickSpeed.NORMAL);
    }

    @Test
    void presetsReadAsWholeTicksPerSecond() {
        assertThat(TickSpeed.PAUSED.ticksPerSecond()).isZero();
        assertThat(TickSpeed.NORMAL.ticksPerSecond()).isEqualTo(20);
        assertThat(TickSpeed.FAST.ticksPerSecond()).isEqualTo(67);
        assertThat(TickSpeed.SUPER_FAST.ticksPerSecond()).isEqualTo(333);
    }

    @Test
    void nudgingMovesTheShownRateByWholeTicks() {
        assertThat(TickSpeed.FAST.nudged(-1).ticksPerSecond()).isEqualTo(66);
        assertThat(TickSpeed.NORMAL.nudged(5)).isEqualTo(TickSpeed.ofTicksPerSecond(25));
        assertThat(TickSpeed.PAUSED.nudged(1).isPaused()).isFalse();
    }

    @Test
    void nudgingStopsAtPaused() {
        TickSpeed slowed = TickSpeed.ofTicksPerSecond(3).nudged(-5);

        assertThat(slowed).isEqualTo(TickSpeed.PAUSED);
        assertThat(slowed.isPaused()).isTrue();
    }
}
