package td.economy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EconomyDeltaTest {

    @Test
    void noneIsTheIdentityElementOnBothSides() {
        EconomyDelta delta = new EconomyDelta(5, 7, -1);

        assertThat(EconomyDelta.none().plus(delta)).isEqualTo(delta);
        assertThat(delta.plus(EconomyDelta.none())).isEqualTo(delta);
    }

    @Test
    void plusIsAssociativeAcrossThreeDeltas() {
        EconomyDelta a = EconomyDelta.credits(10);
        EconomyDelta b = EconomyDelta.score(3);
        EconomyDelta c = EconomyDelta.lives(-1);

        assertThat(a.plus(b).plus(c)).isEqualTo(a.plus(b.plus(c)));
    }

    @Test
    void killCreditsAndScoresTheSameBounty() {
        EconomyDelta delta = EconomyDelta.kill(7);

        assertThat(delta.credits()).isEqualTo(7);
        assertThat(delta.score()).isEqualTo(7);
        assertThat(delta.lives()).isZero();
    }

    @Test
    void leakDeductsScoreAndCostsExactlyOneLife() {
        EconomyDelta delta = EconomyDelta.leak(10);

        assertThat(delta.credits()).isZero();
        assertThat(delta.score()).isEqualTo(-10);
        assertThat(delta.lives()).isEqualTo(-1);
    }
}
