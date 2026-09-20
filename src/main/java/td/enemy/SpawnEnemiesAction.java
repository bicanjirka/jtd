package td.enemy;

import java.util.Optional;

/**
 * Spawns {@code shape.members()} instances of the {@link EnemyCatalog} definition named
 * {@code definitionId}, shaped by {@link AbilitySpawnShape} (member count, per-member
 * multipliers, an optional trait override, and inter-member delay spacing). When
 * {@code consumesSelf} is {@code true}, the mob whose ability fired this is removed from the
 * roster in the same step (via {@link EnemySpawner#replace}) rather than staying alive alongside
 * its spawn - a transformation, not a kill: no bounty, no score, no kill-count-gate credit,
 * mirroring how {@code EnemyRoster.clear()} already removes mobs without notifying
 * {@code GameHost.enemyDied}. The boss egg's hatch-on-timeout ability is the only v1 use of
 * {@code consumesSelf}; the Warden's own on-death spawn doesn't need it, since a death already
 * removes the mob through the existing kill path.
 * <p>
 * The 3-arg constructor is the pre-{@link AbilitySpawnShape} shape every existing caller (a
 * single reinforcement, an egg hatch) still uses - {@code count} becomes
 * {@code shape.members()}, every multiplier stays {@code 1f}, no delay, no trait override.
 * <p>
 * {@link EnemyCatalog#register} walks every definition's {@code SpawnEnemiesAction} references
 * and rejects (via {@code GameStartupException}) any definition that, directly or transitively,
 * could spawn itself - a finite, strictly linear chain (the Warden/egg's 6 stages) is fine; an
 * actual cycle is not. The {@code count}/{@code consumesSelf} combination is only proven for
 * {@code shape.members() == 1} - every v1 use with {@code consumesSelf == true} spawns exactly
 * one replacement; a multi-member consuming spawn has no defined meaning (this mob can only be
 * replaced by one thing) and isn't validated against.
 */
public record SpawnEnemiesAction(String definitionId, AbilitySpawnShape shape, boolean consumesSelf)
        implements AbilityAction {

    public SpawnEnemiesAction(String definitionId, int count, boolean consumesSelf) {
        this(definitionId, new AbilitySpawnShape(count, 1f, 1f, 1f, 0.0, Optional.empty()), consumesSelf);
    }
}
