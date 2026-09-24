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
}
