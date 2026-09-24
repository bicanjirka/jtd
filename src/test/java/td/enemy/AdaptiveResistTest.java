package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.damage.DamageMix;

import static org.assertj.core.api.Assertions.assertThat;

class AdaptiveResistTest {

    private static final TraitContext FULL_HEALTH = new TraitContext(1f);

    @Test
    void anEvenOrEmptyMixResolvesToNoResistAtAll() {
        AdaptiveResist adaptive = new AdaptiveResist(0.8f);
        DamageMix even = DamageMix.of(Damage.physical(10)).plus(DamageMix.of(Damage.magic(10)));

        assertThat(adaptive.resolvedFor(DamageMix.none())).isEmpty();
        assertThat(adaptive.resolvedFor(even)).isEmpty();
    }

    @Test
    void aOneSidedMixResolvesToTheFullResistAgainstThatTypeOnly() {
        Trait resist = new AdaptiveResist(0.8f).resolvedFor(DamageMix.of(Damage.magic(10))).orElseThrow();

        assertThat(resist.onHit(Damage.magic(100), FULL_HEALTH)).isEqualTo(Damage.magic(80));
        assertThat(resist.onHit(Damage.physical(100), FULL_HEALTH)).isEqualTo(Damage.physical(100));
        assertThat(resist.marker()).isEqualTo(TraitMarker.MAGIC_RESIST);
    }

    @Test
    void aPartlyOneSidedMixResolvesToAProportionallyWeakerResist() {
        DamageMix threeToOne = DamageMix.of(Damage.physical(75)).plus(DamageMix.of(Damage.magic(25)));

        Trait resist = new AdaptiveResist(0.8f).resolvedFor(threeToOne).orElseThrow();

        assertThat(resist.onHit(Damage.physical(100), FULL_HEALTH)).isEqualTo(Damage.physical(90));
        assertThat(resist.marker()).isEqualTo(TraitMarker.PHYSICAL_RESIST);
    }
}
