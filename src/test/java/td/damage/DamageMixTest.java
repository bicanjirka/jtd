package td.damage;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DamageMixTest {

    @Test
    void noDamageAndAnEvenSplitLeanTowardNeitherType() {
        DamageMix even = DamageMix.of(Damage.physical(50)).plus(DamageMix.of(Damage.magic(50)));

        assertThat(DamageMix.none().lean(DamageType.PHYSICAL)).isZero();
        assertThat(even.lean(DamageType.PHYSICAL)).isZero();
        assertThat(even.lean(DamageType.MAGIC)).isZero();
    }

    @Test
    void aOneSidedMixLeansFullyTowardThatType() {
        DamageMix mix = DamageMix.of(Damage.magic(30));

        assertThat(mix.dominant()).isEqualTo(DamageType.MAGIC);
        assertThat(mix.lean(DamageType.MAGIC)).isEqualTo(1f);
    }

    @Test
    void aThreeToOneMixLeansHalfwayTowardTheLargerTypeAndNotAtAllTowardTheOther() {
        DamageMix mix = DamageMix.of(Damage.physical(75)).plus(DamageMix.of(Damage.magic(25)));

        assertThat(mix.dominant()).isEqualTo(DamageType.PHYSICAL);
        assertThat(mix.lean(DamageType.PHYSICAL)).isEqualTo(0.5f);
        assertThat(mix.lean(DamageType.MAGIC)).isZero();
    }

    @Test
    void noneIsTheIdentityOfPlus() {
        DamageMix mix = DamageMix.of(Damage.physical(7));

        assertThat(mix.plus(DamageMix.none())).isEqualTo(mix);
        assertThat(DamageMix.none().plus(mix)).isEqualTo(mix);
    }
}
