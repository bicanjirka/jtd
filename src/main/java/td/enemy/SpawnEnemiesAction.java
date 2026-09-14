package td.enemy;

/**
 * Spawns {@code count} instances of the {@link EnemyCatalog} definition named
 * {@code definitionId}. When {@code consumesSelf} is {@code true}, the mob whose ability fired
 * this is removed from the roster in the same step (via {@link EnemySpawner#replace}) rather
 * than staying alive alongside its spawn - a transformation, not a kill: no bounty, no score,
 * no kill-count-gate credit, mirroring how {@code EnemyRoster.clear()} already removes mobs
 * without notifying {@code GameHost.enemyDied}. The boss egg's hatch-on-timeout ability is the
 * only v1 use of {@code consumesSelf}; the Warden's own on-death spawn doesn't need it, since a
 * death already removes the mob through the existing kill path.
 * <p>
 * {@link EnemyCatalog#register} walks every definition's {@code SpawnEnemiesAction} references
 * and rejects (via {@code GameStartupException}) any definition that, directly or transitively,
 * could spawn itself - a finite, strictly linear chain (the Warden/egg's 6 stages) is fine; an
 * actual cycle is not.
 */
public record SpawnEnemiesAction(String definitionId, int count, boolean consumesSelf) implements AbilityAction {
}
