package td.enemy;

import org.junit.jupiter.api.Test;
import td.fixtures.WorldFixtures;
import td.util.GameWorld;
import td.wave.PathNormal;
import td.wave.Vec2;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the payoff of {@link AbilitySpawnShape#delaySpacingSlots} end to end: a multi-member
 * ability spawn's first member is placed active, but later members start {@link
 * DefinedEnemyMob#isInactive()} and only become valid targets once their own delay elapses - what
 * keeps them from arriving stacked on the caster's exact position. Uses its own small catalog
 * rather than a {@code BuiltInEnemies} entry, since this proves the mechanism, not any one boss.
 */
class MultiMemberAbilitySpawnTest {

    @Test
    void laterMembersStartInactiveAndBecomeValidTargetsOnlyAfterTheirOwnDelay() {
        EnemyCatalog catalog = new EnemyCatalog();
        catalog.register(EnemyDefinition.of("spawnling", "Spawnling", 50, 1, 1.28f, BodyArchetype.CIRCLE));
        AbilitySpawnShape shape = new AbilitySpawnShape(3, 2.0);
        catalog.register(EnemyDefinition.of("spawner", "Spawner", 100, 5, 1.28f, BodyArchetype.CIRCLE)
                .withIdentifiedAbilities(List.of(IdentifiedAbility.named("spawn",
                        new Ability(new OnceTrigger(1), new SpawnEnemiesAction("spawnling", shape, false))))));

        GameWorld world = WorldFixtures.newWorldOnBoard(32, 1000, 1000);
        world.setPath(new PathNormal(List.of(new Vec2(0, 0), new Vec2(1000, 0))));
        world.setEnemyCatalog(catalog);
        DefinedEnemyMob spawner = (DefinedEnemyMob) world.getEnemyCatalog().spawn("spawner", world, 0, 100, 5, Rank.GRUNT);
        world.enemies().add(spawner);

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
