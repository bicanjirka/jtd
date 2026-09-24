package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.fixtures.EnemyFixtures;

import static org.assertj.core.api.Assertions.assertThat;

class FlatResistTraitTest {

    @Test
    void damageTakenIsReducedByTheFlatAmountAndClampedAtZero() {
        FlatResistTrait resist = new FlatResistTrait(30);

        assertThat(EnemyFixtures.landedThrough(resist, Damage.physical(100))).isEqualTo(Damage.physical(70));
        assertThat(EnemyFixtures.landedThrough(resist, Damage.physical(10))).isEqualTo(Damage.physical(0));
    }

    @Test
    void aPhysicalOnlyResistanceReducesPhysicalDamageButLeavesMagicUntouched() {
        FlatResistTrait resist = FlatResistTrait.physicalOnly(30);

        assertThat(EnemyFixtures.landedThrough(resist, Damage.physical(100))).isEqualTo(Damage.physical(70));
        assertThat(EnemyFixtures.landedThrough(resist, Damage.magic(100))).isEqualTo(Damage.magic(100));
    }
}
