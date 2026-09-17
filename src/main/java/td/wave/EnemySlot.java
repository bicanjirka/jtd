package td.wave;

import td.enemy.EnemyDefinition;

/**
 * A real enemy spawn - {@link Wave} builds {@code shape.members()} live mobs from
 * {@code definition} for this slot, shaped by {@code shape} (formation, multipliers, spacing).
 */
public record EnemySlot(EnemyDefinition definition, SpawnShape shape) implements WaveSlot {
}
