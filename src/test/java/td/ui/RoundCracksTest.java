package td.ui;

import org.junit.jupiter.api.Test;
import td.enemy.Rank;
import td.level.LevelOutcome;
import td.wave.RoundSummary;
import td.wave.WaveProgress;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoundCracksTest {

    private final RoundCracks cracks = new RoundCracks();

    private static RoundStrip strip(int index, boolean waveReady, int count) {
        WaveProgress progress = new WaveProgress(index, Collections.nCopies(count, new RoundSummary(Rank.GRUNT, false, false)),
                List.of(), List.of());
        return RoundStrip.of(progress, waveReady, LevelOutcome.PLAYING);
    }

    @Test
    void aRoundGoingOverCracksItsSkullForAMomentThenSettles() {
        this.cracks.observe(strip(1, false, 5), 0L);

        this.cracks.observe(strip(1, true, 5), 1_000L);

        assertThat(this.cracks.progress(0, 1_000L)).isZero();
        assertThat(this.cracks.progress(0, 1_000L + RoundCracks.DURATION_MS / 2)).isBetween(0.4f, 0.6f);
        assertThat(this.cracks.progress(0, 1_000L + RoundCracks.DURATION_MS)).isEqualTo(1f);
        assertThat(this.cracks.active(1_000L + RoundCracks.DURATION_MS)).isFalse();
    }

    @Test
    void aRoundStartingCracksNothing() {
        this.cracks.observe(strip(1, true, 5), 0L);

        this.cracks.observe(strip(2, false, 5), 1_000L);

        assertThat(this.cracks.active(1_000L)).isFalse();
    }

    @Test
    void theFirstStripOfALevelIsABaselineEvenWithRoundsAlreadyOver() {
        this.cracks.observe(strip(3, false, 5), 0L);
        this.cracks.reset();

        this.cracks.observe(strip(4, true, 5), 1_000L);

        assertThat(this.cracks.active(1_000L)).isFalse();
    }

    @Test
    void anotherLevelsStripCracksNothing() {
        this.cracks.observe(strip(1, false, 5), 0L);

        this.cracks.observe(strip(4, true, 8), 1_000L);

        assertThat(this.cracks.active(1_000L)).isFalse();
    }
}
