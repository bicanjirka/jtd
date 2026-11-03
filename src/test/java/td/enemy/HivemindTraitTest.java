package td.enemy;

import org.junit.jupiter.api.Test;
import td.damage.Damage;
import td.fixtures.EnemyFixtures;
import td.fixtures.WorldFixtures;
import td.stat.BaseStats;
import td.stat.EnemyStat;
import td.stat.StatSheet;
import td.util.GameWorld;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HivemindTraitTest {

    private static final float PER_ALLY = 6f;
    private static final int MOST = 3;

    private static float armorWith(int alliesNearby) {
        HivemindTrait trait = new HivemindTrait(PER_ALLY, MOST);
        StatSheet sheet = new StatSheet(BaseStats.defaults(EnemyStat.class),
                accumulator -> trait.modifiers(new TraitContext(1f, alliesNearby)).applyTo(accumulator));
        return sheet.value(EnemyStat.ARMOR);
    }

    @Test
    void eachAllyNearbyAddsItsShareOfArmorUpToTheMost() {
        float alone = armorWith(0);

        assertThat(armorWith(2) - alone).isEqualTo(2 * PER_ALLY);
        assertThat(armorWith(MOST + 4) - alone).isEqualTo(MOST * PER_ALLY);
    }

    @Test
    void aLiveMobCountsTheAlliesBesideItAndLosesTheBonusWhenTheyFall() {
        GameWorld world = WorldFixtures.newWorldOnStraightPath();
        DefinedEnemyMob drone = EnemyFixtures.liveMob(world, EnemyFixtures.simpleDefinition("drone")
                .withTraits(List.of(new HivemindTrait(PER_ALLY, MOST))));
        DefinedEnemyMob ally = EnemyFixtures.plainMob(world, 50, 1);
        float before = drone.stats().value(EnemyStat.ARMOR);
        EnemyFixtures.tickEnemies(world, 1, 1);
        float beside = drone.stats().value(EnemyStat.ARMOR);

        ally.doDamage(Damage.physical(ally.getHealth()));
        EnemyFixtures.tickEnemies(world, 2, 2);

        assertThat(beside - before).isEqualTo(PER_ALLY);
        assertThat(drone.stats().value(EnemyStat.ARMOR)).isEqualTo(before);
    }
}
