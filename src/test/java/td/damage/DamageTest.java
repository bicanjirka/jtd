package td.damage;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DamageTest {

    @Test
    void noneIsTheIdentityElementOnBothSides() {
        Damage damage = Damage.physical(40);

        assertThat(Damage.none().plus(damage)).isEqualTo(damage);
        assertThat(damage.plus(Damage.none())).isEqualTo(damage);
    }

    @Test
    void plusIsAssociativeAcrossThreeAmounts() {
        Damage a = Damage.physical(10);
        Damage b = Damage.physical(20);
        Damage c = Damage.physical(30);

        assertThat(a.plus(b).plus(c)).isEqualTo(a.plus(b.plus(c)));
    }

    @Test
    void constructionClampsNegativeAmountsToZero() {
        assertThat(Damage.physical(-5).amount()).isZero();
        assertThat(new Damage(-5, DamageType.PHYSICAL).amount()).isZero();
    }

    @Test
    void scaledByAppliesTheFactorAndRoundsToTheNearestInt() {
        assertThat(Damage.physical(1000).scaledBy(0.75f).amount()).isEqualTo(750);
    }

    @Test
    void scalingByANegativeFactorClampsAtZeroRatherThanHealing() {
        assertThat(Damage.physical(100).scaledBy(-0.5f).amount()).isZero();
    }

    @Test
    void scaledByPreservesType() {
        assertThat(Damage.magic(1000).scaledBy(0.5f).type()).isEqualTo(DamageType.MAGIC);
    }

    @Test
    void aZeroAmountDamageIsTheIdentityRegardlessOfEitherSidesType() {
        Damage physical = Damage.physical(40);

        assertThat(Damage.magic(0).plus(physical)).isEqualTo(physical);
        assertThat(physical.plus(Damage.magic(0))).isEqualTo(physical);
    }

    @Test
    void combiningTwoNonZeroDamagesOfDifferentTypesThrows() {
        Damage physical = Damage.physical(10);
        Damage magic = Damage.magic(10);

        assertThatThrownBy(() -> physical.plus(magic)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cappedAtLimitsTheAmountAndPreservesType() {
        Damage capped = Damage.magic(100).cappedAt(30);

        assertThat(capped.amount()).isEqualTo(30);
        assertThat(capped.type()).isEqualTo(DamageType.MAGIC);
    }

    @Test
    void cappedAtAboveTheCurrentAmountLeavesItUnchanged() {
        assertThat(Damage.physical(30).cappedAt(100)).isEqualTo(Damage.physical(30));
    }
}
