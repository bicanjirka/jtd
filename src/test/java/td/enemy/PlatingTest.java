package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.fixtures.EnemyFixtures;

import static org.assertj.core.api.Assertions.assertThat;

class PlatingTest {

    @Test
    void damageTakenIsReducedByTheFlatAmountAndClampedAtZero() {
        EnemyDefinition plated = EnemyFixtures.simpleDefinition("plated").withPhysicalPlating(0.3f);

        assertThat(EnemyFixtures.landedThrough(plated, Damage.physical(100))).isEqualTo(Damage.physical(70));
        assertThat(EnemyFixtures.landedThrough(plated, Damage.physical(10))).isEqualTo(Damage.physical(0));
    }

    @Test
    void physicalPlatingLeavesMagicUntouchedAndMagicPlatingLeavesPhysical() {
        EnemyDefinition plated = EnemyFixtures.simpleDefinition("plated").withPhysicalPlating(0.3f);
        EnemyDefinition warded = EnemyFixtures.simpleDefinition("warded").withMagicPlating(0.3f);

        assertThat(EnemyFixtures.landedThrough(plated, Damage.magic(100))).isEqualTo(Damage.magic(100));
        assertThat(EnemyFixtures.landedThrough(warded, Damage.magic(100))).isEqualTo(Damage.magic(70));
        assertThat(EnemyFixtures.landedThrough(warded, Damage.physical(100))).isEqualTo(Damage.physical(100));
    }
}
