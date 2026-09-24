package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.damage.DamageMix;
import td.damage.DamageType;
import td.fixtures.EnemyFixtures;

import static org.assertj.core.api.Assertions.assertThat;

class AdaptiveResistTest {
    private static final AdaptiveResist PLATING = AdaptiveResist.against(DamageType.PHYSICAL, 0.8f, 0.2f);

    @Test
    void againstDominantResolvesToNoResistAtAnEvenOrEmptyMix() {
        AdaptiveResist adaptive = AdaptiveResist.againstDominant(0.8f);
        DamageMix even = DamageMix.of(Damage.physical(10)).plus(DamageMix.of(Damage.magic(10)));

        assertThat(adaptive.resolvedFor(DamageMix.none())).isEmpty();
        assertThat(adaptive.resolvedFor(even)).isEmpty();
    }

    @Test
    void againstDominantResolvesToTheFullResistAgainstAOneSidedMixsTypeOnly() {
        Trait resist = AdaptiveResist.againstDominant(0.8f).resolvedFor(DamageMix.of(Damage.magic(10))).orElseThrow();

        assertThat(EnemyFixtures.landedThrough(resist, Damage.magic(100))).isEqualTo(Damage.magic(80));
        assertThat(EnemyFixtures.landedThrough(resist, Damage.physical(100))).isEqualTo(Damage.physical(100));
        assertThat(resist.marker()).isEqualTo(TraitMarker.MAGIC_RESIST);
    }

    @Test
    void againstDominantResolvesToAProportionallyWeakerResistAtAPartlyOneSidedMix() {
        DamageMix threeToOne = DamageMix.of(Damage.physical(75)).plus(DamageMix.of(Damage.magic(25)));

        Trait resist = AdaptiveResist.againstDominant(0.8f).resolvedFor(threeToOne).orElseThrow();

        assertThat(EnemyFixtures.landedThrough(resist, Damage.physical(100))).isEqualTo(Damage.physical(90));
        assertThat(resist.marker()).isEqualTo(TraitMarker.PHYSICAL_RESIST);
    }

    @Test
    void aFixedTypeResistKeepsItsEvenStrengthWithNoDamageAnEvenSplitOrAMagicHeavyMix() {
        DamageMix even = DamageMix.of(Damage.physical(10)).plus(DamageMix.of(Damage.magic(10)));
        DamageMix allMagic = DamageMix.of(Damage.magic(10));

        for (DamageMix mix : new DamageMix[] {DamageMix.none(), even, allMagic}) {
            Trait resist = PLATING.resolvedFor(mix).orElseThrow();
            assertThat(EnemyFixtures.landedThrough(resist, Damage.physical(100))).isEqualTo(Damage.physical(80));
        }
    }

    @Test
    void aFixedTypeResistHardensLinearlyToItsFullStrengthAsTheMixLeansTowardItsType() {
        DamageMix threeToOne = DamageMix.of(Damage.physical(75)).plus(DamageMix.of(Damage.magic(25)));

        Trait halfway = PLATING.resolvedFor(threeToOne).orElseThrow();
        Trait full = PLATING.resolvedFor(DamageMix.of(Damage.physical(10))).orElseThrow();

        assertThat(EnemyFixtures.landedThrough(halfway, Damage.physical(100))).isEqualTo(Damage.physical(50));
        assertThat(EnemyFixtures.landedThrough(full, Damage.physical(100))).isEqualTo(Damage.physical(20));
        assertThat(EnemyFixtures.landedThrough(full, Damage.magic(100))).isEqualTo(Damage.magic(100));
    }
}
