package td.tower.sniper;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SniperTempoTest {

    private final SniperTempo tempo = new SniperTempo();

    @Test
    void withNothingRunningTheWaitIsUnchanged() {
        assertThat(this.tempo.takeAttackSpeedBonus(0)).isZero();
    }

    @Test
    void aFrenzyHalvesTheNextThreeWaitsOnly() {
        this.tempo.startFrenzy(0);

        assertThat(this.tempo.takeAttackSpeedBonus(1)).isEqualTo(1f);
        assertThat(this.tempo.takeAttackSpeedBonus(2)).isEqualTo(1f);
        assertThat(this.tempo.takeAttackSpeedBonus(3)).isEqualTo(1f);
        assertThat(this.tempo.takeAttackSpeedBonus(4)).isZero();
    }

    @Test
    void aFrenzyOfTwiceTheLengthHalvesTheNextSixWaits() {
        this.tempo.startFrenzy(0, 2f);

        for (int tick = 1; tick <= 6; tick++) {
            assertThat(this.tempo.takeAttackSpeedBonus(tick)).isEqualTo(1f);
        }
        assertThat(this.tempo.takeAttackSpeedBonus(7)).isZero();
    }

    @Test
    void aFrenzyStartedWhileOneRunsDoesNotRestartIt() {
        this.tempo.startFrenzy(0);
        this.tempo.takeAttackSpeedBonus(1);

        this.tempo.startFrenzy(2);

        assertThat(this.tempo.takeAttackSpeedBonus(3)).isEqualTo(1f);
        assertThat(this.tempo.takeAttackSpeedBonus(4)).isEqualTo(1f);
        assertThat(this.tempo.takeAttackSpeedBonus(5)).isZero();
    }

    @Test
    void aBurstHalvesEveryWaitUntilItEnds() {
        this.tempo.startBurst(100);

        assertThat(this.tempo.takeAttackSpeedBonus(10)).isEqualTo(1f);
        assertThat(this.tempo.takeAttackSpeedBonus(99)).isEqualTo(1f);
        assertThat(this.tempo.takeAttackSpeedBonus(100)).isZero();
    }

    @Test
    void aBurstReplacesARunningFrenzyInsteadOfAddingToIt() {
        this.tempo.startFrenzy(0);
        this.tempo.startBurst(10);

        assertThat(this.tempo.takeAttackSpeedBonus(10)).isZero();
    }

    @Test
    void aFrenzyNeverStartsOverARunningBurst() {
        this.tempo.startBurst(10);
        this.tempo.startFrenzy(5);

        assertThat(this.tempo.takeAttackSpeedBonus(10)).isZero();
    }
}
