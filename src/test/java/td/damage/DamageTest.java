package td.damage;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DamageTest {

    @Test
    void noneIsTheIdentityElementOnBothSides() {
        Damage damage = Damage.of(40);

        assertThat(Damage.none().plus(damage)).isEqualTo(damage);
        assertThat(damage.plus(Damage.none())).isEqualTo(damage);
    }

    @Test
    void plusIsAssociativeAcrossThreeAmounts() {
        Damage a = Damage.of(10);
        Damage b = Damage.of(20);
        Damage c = Damage.of(30);

        assertThat(a.plus(b).plus(c)).isEqualTo(a.plus(b.plus(c)));
    }

    @Test
    void constructionClampsNegativeAmountsToZero() {
        assertThat(Damage.of(-5).amount()).isZero();
        assertThat(new Damage(-5).amount()).isZero();
    }

    @Test
    void scaledByAppliesTheFactorAndRoundsToTheNearestInt() {
        assertThat(Damage.of(1000).scaledBy(0.75f).amount()).isEqualTo(750);
    }

    @Test
    void scalingByANegativeFactorClampsAtZeroRatherThanHealing() {
        assertThat(Damage.of(100).scaledBy(-0.5f).amount()).isZero();
    }
}
