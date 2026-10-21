package td.wave;

import td.enemy.EnemyDefinition;
import td.enemy.Rank;

/**
 * A real spawn: {@code shape.members()} mobs of {@code definition}. {@code rank} is already
 * resolved against the enemy's ladder, so it always matches {@code definition}.
 *
 * @param unique whether the enemy exists only at Boss rank
 */
public record EnemySlot(EnemyDefinition definition, Rank rank, SpawnShape shape, boolean unique) implements WaveSlot {

    /** An enemy with a rank ladder. */
    public EnemySlot(EnemyDefinition definition, Rank rank, SpawnShape shape) {
        this(definition, rank, shape, false);
    }
}
