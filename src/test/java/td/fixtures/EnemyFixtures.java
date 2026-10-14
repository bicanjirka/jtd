package td.fixtures;

import td.damage.Damage;
import td.enemy.Ability;
import td.enemy.BodyArchetype;
import td.enemy.EnemyDefinition;
import td.enemy.EnemyMob;
import td.enemy.HitResolution;
import td.enemy.OnDeathTrigger;
import td.enemy.SpawnEnemiesAction;
import td.enemy.Trait;
import td.enemy.TraitContext;
import td.stat.BaseStats;
import td.stat.EnemyStat;
import td.stat.StatSheet;
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

    /** What {@code damage} lands for against an otherwise plain enemy carrying only {@code trait}. */
    public static Damage landedThrough(Trait trait, Damage damage) {
        StatSheet sheet = new StatSheet(BaseStats.defaults(EnemyStat.class),
                accumulator -> trait.modifiers(new TraitContext(1f)).applyTo(accumulator));
        return HitResolution.resolve(damage, sheet);
    }

    /** What {@code damage} lands for against an enemy with {@code definition}'s base stats and no traits. */
    public static Damage landedThrough(EnemyDefinition definition, Damage damage) {
        return HitResolution.resolve(damage, new StatSheet(definition.baseStats(), accumulator -> {
        }));
    }

    /** Replaces a world's live enemies with exactly these mobs. */
    public static void spawn(GameWorld world, EnemyMob... mobs) {
        world.enemies().setEnemies(mobs);
    }
}
