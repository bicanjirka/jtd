package td.wave;

import td.enemy.EnemyDefinition;
import td.enemy.Rank;

/**
 * A real enemy spawn - {@link Wave} builds {@code shape.members()} live mobs from
 * {@code definition} for this slot, shaped by {@code shape} (formation, multipliers, spacing).
 * {@code rank} is this slot's own <em>effective</em> rank - the wave's default, or the slot's own
 * override token, already resolved against this enemy's own authored ladder (see
 * {@code td.enemy.RankedEnemy}'s fallback rule) by the time {@code WaveScript} builds this slot,
 * so {@code definition} and {@code rank} always agree with each other.
 */
public record EnemySlot(EnemyDefinition definition, Rank rank, SpawnShape shape) implements WaveSlot {
}
