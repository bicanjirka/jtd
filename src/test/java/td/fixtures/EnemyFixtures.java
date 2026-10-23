package td.fixtures;

import td.damage.Damage;
import td.enemy.Ability;
import td.enemy.AbilityTerm;
import td.enemy.BodyArchetype;
import td.enemy.DefinedEnemyMob;
import td.enemy.EnemyDefinition;
import td.enemy.EnemyMob;
import td.enemy.HitResolution;
import td.enemy.OnDeathTrigger;
import td.enemy.Rank;
import td.enemy.SpawnEnemiesAction;
import td.enemy.SpawnParameters;
import td.enemy.Trait;
import td.enemy.TraitContext;
import td.stat.BaseStats;
import td.stat.EnemyStat;
import td.stat.StatSheet;
import td.util.GameWorld;

import java.util.List;

/**
 * Enemy definitions built in the test and the live mobs made from them, so a mechanic is tested
 * apart from any built-in enemy.
 */
public final class EnemyFixtures {

    private EnemyFixtures() {
    }

    public static EnemyDefinition simpleDefinition(String id) {
        return EnemyDefinition.of(id, id, 100, 5, 1.28f, BodyArchetype.CIRCLE);
    }

    /** A minimal definition that spawns {@code spawnedId} on death. */
    public static EnemyDefinition definitionThatSpawns(String id, String spawnedId) {
        Ability spawnOnDeath = new Ability(AbilityTerm.GUARD, new OnDeathTrigger(), new SpawnEnemiesAction(spawnedId, 1, false));
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

    /**
     * A live Grunt of {@code definition} at the start of the world's path, among its enemies, at the
     * definition's own health and bounty. Nothing is registered: a test whose mob spawns others by id
     * registers those in the world's catalog.
     */
    public static DefinedEnemyMob liveMob(GameWorld world, EnemyDefinition definition) {
        return liveMob(world, definition, Rank.GRUNT);
    }

    /** {@link #liveMob(GameWorld, EnemyDefinition)} wearing {@code rank}, which changes nothing else about it. */
    public static DefinedEnemyMob liveMob(GameWorld world, EnemyDefinition definition, Rank rank) {
        DefinedEnemyMob mob = new DefinedEnemyMob(definition, world,
                SpawnParameters.atSlot(0, definition.baseSpeed(), definition.baseHealth(), definition.price()), rank);
        world.enemies().add(mob);
        return mob;
    }

    /** A live mob with no stats, traits or abilities: any enemy, for a test about something else. */
    public static DefinedEnemyMob plainMob(GameWorld world, int healthPoints, int bounty) {
        return liveMob(world, simpleDefinition("plain").withHealthAndPrice(healthPoints, bounty));
    }

    /** Ticks every enemy of the world, those spawned meanwhile included, from {@code fromTick} to {@code toTick}. */
    public static void tickEnemies(GameWorld world, int fromTick, int toTick) {
        for (int t = fromTick; t <= toTick; t++) {
            for (EnemyMob enemy : world.enemies().getEnemies()) {
                enemy.doTick(t);
            }
        }
    }

    /** Replaces a world's live enemies with exactly these mobs. */
    public static void spawn(GameWorld world, EnemyMob... mobs) {
        world.enemies().setEnemies(mobs);
    }
}
