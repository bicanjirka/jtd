package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.fixtures.EnemyFixtures;

import static org.assertj.core.api.Assertions.assertThat;

class ArmorTest {

    @Test
    void armorLeavesMagicUntouchedAndMagicResistLeavesPhysical() {
        EnemyDefinition armored = EnemyFixtures.simpleDefinition("armored").withArmor(100f);
        EnemyDefinition warded = EnemyFixtures.simpleDefinition("warded").withMagicResist(100f);

        assertThat(EnemyFixtures.landedThrough(armored, Damage.physical(1000))).isEqualTo(Damage.physical(500));
        assertThat(EnemyFixtures.landedThrough(armored, Damage.magic(1000))).isEqualTo(Damage.magic(1000));
        assertThat(EnemyFixtures.landedThrough(warded, Damage.magic(1000))).isEqualTo(Damage.magic(500));
        assertThat(EnemyFixtures.landedThrough(warded, Damage.physical(1000))).isEqualTo(Damage.physical(1000));
    }
}
