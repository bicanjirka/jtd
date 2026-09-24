package td.damage;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DamageMixTest {

    @Test
    void noDamageAndAnEvenSplitAreNotDominatedByEitherType() {
        DamageMix even = DamageMix.of(Damage.physical(50)).plus(DamageMix.of(Damage.magic(50)));

        assertThat(DamageMix.none().dominance()).isZero();
        assertThat(even.dominance()).isZero();
    }

    @Test
    void aOneSidedMixIsFullyDominatedByThatType() {
        DamageMix mix = DamageMix.of(Damage.magic(30));

        assertThat(mix.dominant()).isEqualTo(DamageType.MAGIC);
        assertThat(mix.dominance()).isEqualTo(1f);
    }

    @Test
    void aThreeToOneMixIsHalfwayDominated() {
        DamageMix mix = DamageMix.of(Damage.physical(75)).plus(DamageMix.of(Damage.magic(25)));

        assertThat(mix.dominant()).isEqualTo(DamageType.PHYSICAL);
        assertThat(mix.dominance()).isEqualTo(0.5f);
    }

    @Test
    void noneIsTheIdentityOfPlus() {
        DamageMix mix = DamageMix.of(Damage.physical(7));

        assertThat(mix.plus(DamageMix.none())).isEqualTo(mix);
        assertThat(DamageMix.none().plus(mix)).isEqualTo(mix);
    }
}
