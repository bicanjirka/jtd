package td.enemy;

import org.junit.jupiter.api.Test;
import td.fixtures.EnemyFixtures;
import td.fixtures.WorldFixtures;
import td.util.GameWorld;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Uses its own small catalog: it tests the mechanism, not a built-in enemy. */
class MultiMemberAbilitySpawnTest {

    @Test
    void laterMembersStartInactiveAndBecomeValidTargetsOnlyAfterTheirOwnDelay() {
        GameWorld world = WorldFixtures.newWorldOnStraightPath();
        world.getEnemyCatalog().register(EnemyFixtures.simpleDefinition("spawnling"));
        DefinedEnemyMob spawner = EnemyFixtures.liveMob(world, EnemyFixtures.simpleDefinition("spawner")
                .withAbilities(List.of(new Ability(AbilityTerm.GUARD, new OnceTrigger(1),
                        new SpawnEnemiesAction("spawnling", new AbilitySpawnShape(3, 2.0), false)))));

        spawner.doTick(1); // fires the Once-at-1 trigger, spawning all 3 members immediately

        EnemyMob[] enemies = world.enemies().getEnemies();
        assertThat(enemies).hasSize(4); // the spawner plus its 3-member brood
        DefinedEnemyMob first = (DefinedEnemyMob) enemies[1];
        DefinedEnemyMob second = (DefinedEnemyMob) enemies[2];
        DefinedEnemyMob third = (DefinedEnemyMob) enemies[3];

        assertThat(first.isInactive()).isFalse();
        assertThat(second.isInactive()).isTrue();
        assertThat(third.isInactive()).isTrue();

        for (int t = 2; t <= 200 && (second.isInactive() || third.isInactive()); t++) {
            second.doTick(t);
            third.doTick(t);
        }

        assertThat(second.isInactive()).isFalse();
        assertThat(second.validTarget()).isTrue();
        assertThat(third.isInactive()).isFalse();
        assertThat(third.validTarget()).isTrue();
    }
}
