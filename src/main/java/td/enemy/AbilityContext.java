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
     * {@code true} only on the tick this mob is first observed to have survived a critical
     * hit - edge-triggered the same way {@link #justDied()} is, and for the same reason: a
     * hit can land during another phase of the same game tick (see
     * {@code td/enemy/CLAUDE.md}'s death-timing invariant), so this is captured on the mob's
     * own next {@code doTick} rather than read synchronously where the hit landed.
     */
    boolean justTookCriticalHit();

    /**
     * {@code true} only on the tick this mob is first observed to have taken any damage at
     * all (critical or not) - edge-triggered the same way {@link #justTookCriticalHit()} is,
     * and for the same deferred-capture reason (see {@code td/enemy/CLAUDE.md}'s death-timing
     * invariant): a hit can land during another phase of the same game tick, so it is captured
     * on this mob's own next tick rather than read synchronously where the hit landed.
     */
    boolean justTookDamage();

    /**
     * Applies {@code template} to {@code target}, relative to this mob.
     */
    void applyEffect(EffectTemplate template, EffectTarget target);

    /**
     * Spawns {@code count} instances of {@code definitionId}, optionally consuming this mob - see {@link SpawnEnemiesAction}.
     */
    void spawnEnemies(String definitionId, int count, boolean consumesSelf);
}
