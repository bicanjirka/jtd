package td.wave;

import td.enemy.EnemyDefinition;
import td.enemy.Rank;

/**
 * A real spawn: {@code shape.members()} mobs of {@code definition}, its affixes already applied.
 * {@code rank} is already resolved against the enemy's ladder, so it always matches {@code definition}.
 *
 * @param unique whether the enemy exists only at Boss rank
 */
public record EnemySlot(EnemyDefinition definition, Rank rank, SpawnShape shape, boolean unique) implements WaveSlot {
}
