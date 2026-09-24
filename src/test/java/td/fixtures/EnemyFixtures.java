package td.fixtures;

import td.enemy.Ability;
import td.enemy.BodyArchetype;
import td.enemy.EnemyDefinition;
import td.enemy.EnemyMob;
import td.enemy.OnDeathTrigger;
import td.enemy.SpawnEnemiesAction;
import td.util.GameWorld;

import java.util.List;

/** Minimal enemy definitions for tests that don't care about real stats. */
public final class EnemyFixtures {

    private EnemyFixtures() {
    }

    public static EnemyDefinition simpleDefinition(String id) {
        return EnemyDefinition.of(id, id, 100, 5, 1.28f, BodyArchetype.CIRCLE);
    }

    /** A minimal definition that spawns {@code spawnedId} on death. */
    public static EnemyDefinition definitionThatSpawns(String id, String spawnedId) {
        Ability spawnOnDeath = new Ability(new OnDeathTrigger(), new SpawnEnemiesAction(spawnedId, 1, false));
        return simpleDefinition(id).withAbilities(List.of(spawnOnDeath));
    }

    /** Replaces a world's live enemies with exactly these mobs. */
    public static void spawn(GameWorld world, EnemyMob... mobs) {
        world.enemies().setEnemies(mobs);
    }
}
