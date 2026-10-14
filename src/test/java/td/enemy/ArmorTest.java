package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.fixtures.EnemyFixtures;
import td.fixtures.WorldFixtures;
import td.util.GameWorld;

import static org.assertj.core.api.Assertions.assertThat;

class ArmorTest {

    private final GameWorld context = WorldFixtures.newWorld();

    @Test
    void twentyFiveArmorKeepsFourFifthsOfAHit() {
        EnemyMob armored = EnemyFactory.getEnemy("s", context, 0, 100, 5, Rank.GRUNT);
        // healthMax = 100*100 = 10000; 25 magic resist keeps 100/125 of a hit, so 1000 raw damage becomes 800 applied
        armored.doDamage(Damage.magic(1000));

        assertThat(armored.getHealth()).isEqualTo(10000 - 800);
    }

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
