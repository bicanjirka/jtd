package td.wave;

import td.enemy.Rank;

/**
 * One authored wave. Health and bounty come from each enemy's definition at its rank.
 *
 * @param enemies         space-separated tokens, e.g. {@code "3 knight e 4 walker"}
 * @param rank            the default rank; a slot may override it
 * @param speedMultiplier pace, multiplied with the path's; {@code 1x} by default
 * @param clearBonus      credits paid when the round this wave belongs to clears with no enemy leaked; none by
 *                        default
 */
public record WaveDefinition(String enemies, Rank rank, float speedMultiplier, int clearBonus) {

    public WaveDefinition {
        if (speedMultiplier <= 0f) {
            throw new IllegalArgumentException("A wave's speed multiplier must be positive, was " + speedMultiplier);
        }
        if (clearBonus < 0) {
            throw new IllegalArgumentException("A wave's clear bonus must not be negative, was " + clearBonus);
        }
    }

    /** At normal pace, with no clear bonus. */
    public WaveDefinition(String enemies, Rank rank) {
        this(enemies, rank, 1f, 0);
    }

    public WaveDefinition withSpeedMultiplier(float speedMultiplier) {
        return new WaveDefinition(this.enemies, this.rank, speedMultiplier, this.clearBonus);
    }

    public WaveDefinition withClearBonus(int clearBonus) {
        return new WaveDefinition(this.enemies, this.rank, this.speedMultiplier, clearBonus);
    }
}
