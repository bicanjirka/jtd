package td.tower.sniper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SniperTempoTest {

    private final SniperTempo tempo = new SniperTempo();

    @Test
    void withNothingRunningTheWaitIsUnchanged() {
        assertThat(this.tempo.takeFireRateBonus(0)).isZero();
    }

    @Test
    void aFrenzyHalvesTheNextThreeWaitsOnly() {
        this.tempo.startFrenzy(0);

        assertThat(this.tempo.takeFireRateBonus(1)).isEqualTo(0.5f);
        assertThat(this.tempo.takeFireRateBonus(2)).isEqualTo(0.5f);
        assertThat(this.tempo.takeFireRateBonus(3)).isEqualTo(0.5f);
        assertThat(this.tempo.takeFireRateBonus(4)).isZero();
    }

    @Test
    void aFrenzyOfTwiceTheLengthHalvesTheNextSixWaits() {
        this.tempo.startFrenzy(0, 2f);

        for (int tick = 1; tick <= 6; tick++) {
            assertThat(this.tempo.takeFireRateBonus(tick)).isEqualTo(0.5f);
        }
        assertThat(this.tempo.takeFireRateBonus(7)).isZero();
    }

    @Test
    void aFrenzyStartedWhileOneRunsDoesNotRestartIt() {
        this.tempo.startFrenzy(0);
        this.tempo.takeFireRateBonus(1);

        this.tempo.startFrenzy(2);

        assertThat(this.tempo.takeFireRateBonus(3)).isEqualTo(0.5f);
        assertThat(this.tempo.takeFireRateBonus(4)).isEqualTo(0.5f);
        assertThat(this.tempo.takeFireRateBonus(5)).isZero();
    }

    @Test
    void aBurstHalvesEveryWaitUntilItEnds() {
        this.tempo.startBurst(100);

        assertThat(this.tempo.takeFireRateBonus(10)).isEqualTo(0.5f);
        assertThat(this.tempo.takeFireRateBonus(99)).isEqualTo(0.5f);
        assertThat(this.tempo.takeFireRateBonus(100)).isZero();
    }

    @Test
    void aBurstReplacesARunningFrenzyInsteadOfAddingToIt() {
        this.tempo.startFrenzy(0);
        this.tempo.startBurst(10);

        assertThat(this.tempo.takeFireRateBonus(10)).isZero();
    }

    @Test
    void aFrenzyNeverStartsOverARunningBurst() {
        this.tempo.startBurst(10);
        this.tempo.startFrenzy(5);

        assertThat(this.tempo.takeFireRateBonus(10)).isZero();
    }
}
