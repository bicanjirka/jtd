package td.wave;

import td.enemy.Rank;

/**
 * One authored wave. Health and bounty come from each enemy's definition at its rank.
 *
 * @param enemies         space-separated tokens, e.g. {@code "3 s e 4 c"}
 * @param rank            the default rank; a slot may override it
 * @param speedMultiplier pace, multiplied with the path's; {@code 1x} by default
 */
public record WaveDefinition(String enemies, Rank rank, float speedMultiplier) {

    public WaveDefinition {
        if (speedMultiplier <= 0f) {
            throw new IllegalArgumentException("A wave's speed multiplier must be positive, was " + speedMultiplier);
        }
    }

    /** At normal pace. */
    public WaveDefinition(String enemies, Rank rank) {
        this(enemies, rank, 1f);
    }

    public WaveDefinition withSpeedMultiplier(float speedMultiplier) {
        return new WaveDefinition(this.enemies, this.rank, speedMultiplier);
    }
}
