package td.wave;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WaveClockTest {

    @Test
    void aWaveStartingCountsItselfAndKeepsTheTickItStartedOnUntilItEnds() {
        WaveClock clock = WaveClock.first().start(120);

        WaveClock ended = clock.end();

        assertThat(clock).isEqualTo(new WaveClock(1, true, 120));
        assertThat(ended).isEqualTo(new WaveClock(1, false, 120));
    }

    @Test
    void startingOverKeepsTheCountSoAPurchaseStampedBeforeNeverMatchesAgain() {
        WaveClock clock = WaveClock.first().start(40).start(90);

        assertThat(clock.startOver()).isEqualTo(new WaveClock(2, false, 0));
    }
}
