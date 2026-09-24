package td.wave;

import td.enemy.EnemyDefinition;
import td.enemy.Rank;

/**
 * A real spawn: {@code shape.members()} mobs of {@code definition}. {@code rank} is already
 * resolved against the enemy's ladder, so it always matches {@code definition}.
 */
public record EnemySlot(EnemyDefinition definition, Rank rank, SpawnShape shape) implements WaveSlot {
}
