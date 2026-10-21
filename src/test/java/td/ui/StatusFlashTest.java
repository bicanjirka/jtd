package td.ui;

import org.junit.jupiter.api.Test;
import td.economy.StatusTerm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class StatusFlashTest {

    private static StatusReadout readout(int round, int lives, int score, int credits) {
        return new StatusReadout(round, 18, false, false, lives, score, credits);
    }

    private final StatusFlash flash = new StatusFlash();

    @Test
    void theFirstReadoutIsABaselineNotAChange() {
        this.flash.observe(readout(3, 5, 0, 100), 0);

        assertThat(this.flash.strength(StatusTerm.LIVES, 0)).isZero();
        assertThat(this.flash.strength(StatusTerm.WAVE, 0)).isZero();
    }

    @Test
    void aDropInLivesFlashesAtFullStrengthAndFadesEvenly() {
        this.flash.observe(readout(3, 5, 0, 100), 0);

        this.flash.observe(readout(3, 4, 0, 100), 1000);

        assertThat(this.flash.strength(StatusTerm.LIVES, 1000)).isEqualTo(1f);
        assertThat(this.flash.strength(StatusTerm.LIVES, 1150)).isCloseTo(0.5f, within(1e-6f));
        assertThat(this.flash.strength(StatusTerm.LIVES, 1000 + StatusFlash.DURATION_MS)).isZero();
        assertThat(this.flash.strength(StatusTerm.LIVES, 5000)).isZero();
    }

    @Test
    void aRoundStartingFlashesTheWave() {
        this.flash.observe(readout(3, 5, 0, 100), 0);

        this.flash.observe(readout(4, 5, 0, 100), 10);

        assertThat(this.flash.strength(StatusTerm.WAVE, 10)).isEqualTo(1f);
    }

    @Test
    void cashAndScoreNeverFlashHoweverOftenTheyChange() {
        this.flash.observe(readout(3, 5, 0, 100), 0);

        this.flash.observe(readout(3, 5, 40, 140), 10);
        this.flash.observe(readout(3, 5, 10, 90), 20);

        assertThat(this.flash.strength(StatusTerm.SCORE, 20)).isZero();
        assertThat(this.flash.strength(StatusTerm.CASH, 20)).isZero();
        assertThat(this.flash.active(20)).isFalse();
    }

    @Test
    void gainingALifeOrLosingARoundIsNotAFlash() {
        this.flash.observe(readout(3, 5, 0, 100), 0);

        this.flash.observe(readout(2, 6, 0, 100), 10);

        assertThat(this.flash.active(10)).isFalse();
    }

    @Test
    void aLevelStartingIsANewBaselineSoItsLowerLivesDoNotFlash() {
        this.flash.observe(readout(9, 2, 0, 100), 0);
        this.flash.reset();

        this.flash.observe(readout(0, 1, 0, 100), 10);

        assertThat(this.flash.active(10)).isFalse();
    }

    @Test
    void theBarIsActiveWhileAnyIconStillFlashes() {
        this.flash.observe(readout(3, 5, 0, 100), 0);
        this.flash.observe(readout(3, 4, 0, 100), 0);

        assertThat(this.flash.active(100)).isTrue();
        assertThat(this.flash.active(StatusFlash.DURATION_MS)).isFalse();
    }
}
