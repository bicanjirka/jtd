package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.fixtures.EnemyFixtures;

import static org.assertj.core.api.Assertions.assertThat;

class PlatingTest {

    @Test
    void damageTakenIsReducedByTheFlatAmountAndClampedAtZero() {
        EnemyDefinition plated = EnemyFixtures.simpleDefinition("plated").withPlating(0.3f);

        assertThat(EnemyFixtures.landedThrough(plated, Damage.physical(100))).isEqualTo(Damage.physical(70));
        assertThat(EnemyFixtures.landedThrough(plated, Damage.physical(10))).isEqualTo(Damage.physical(0));
    }

    @Test
    void platingLeavesMagicUntouchedAndInsulationLeavesPhysical() {
        EnemyDefinition plated = EnemyFixtures.simpleDefinition("plated").withPlating(0.3f);
        EnemyDefinition insulated = EnemyFixtures.simpleDefinition("insulated").withInsulation(0.3f);

        assertThat(EnemyFixtures.landedThrough(plated, Damage.magic(100))).isEqualTo(Damage.magic(100));
        assertThat(EnemyFixtures.landedThrough(insulated, Damage.magic(100))).isEqualTo(Damage.magic(70));
        assertThat(EnemyFixtures.landedThrough(insulated, Damage.physical(100))).isEqualTo(Damage.physical(100));
    }
}
