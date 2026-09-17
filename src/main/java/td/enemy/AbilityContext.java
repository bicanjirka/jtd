package td.enemy;

import td.effect.EffectTemplate;

/**
 * What {@link AbilityEvaluator} needs to evaluate and fire one {@link Ability} for one mob on
 * one tick - a narrow seam, not a reference to the mob itself, so evaluation logic is testable
 * against a fake without any real {@code EnemyMob} implementation existing yet (see
 * {@code docs/features/FEATURE-enemy-traits-and-effects.md}'s Phase 1). A real implementation (added when the
 * unified enemy model lands) resolves {@link #applyEffect}/{@link #spawnEnemies} against its
 * own {@code EnemyCatalog}/{@link EnemySpawner}/position - {@link AbilityEvaluator} itself never
 * needs to see those directly.
 */
public interface AbilityContext {

    /**
     * Current health / max health, in {@code [0, 1]}.
     */
    float healthFraction();

    /**
     * Ticks elapsed since this mob spawned.
     */
    int ticksSinceSpawn();

    /**
     * Ticks elapsed since this mob last took a hit - {@code 0} on the tick a hit landed.
     */
    int ticksSinceLastHit();

    /**
     * {@code true} only on the tick this mob's death is first observed.
     */
    boolean justDied();

    /**
     * Applies {@code template} to {@code target}, relative to this mob.
     */
    void applyEffect(EffectTemplate template, EffectTarget target);

    /**
     * Spawns {@code count} instances of {@code definitionId}, optionally consuming this mob - see {@link SpawnEnemiesAction}.
     */
    void spawnEnemies(String definitionId, int count, boolean consumesSelf);
}
