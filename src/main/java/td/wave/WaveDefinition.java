package td.wave;

import td.enemy.Rank;

/**
 * One authored wave: its contents as a token string in the wave mini-language (see the table
 * in CLAUDE.md), the three numbers every enemy in it is built from, and its own optional speed
 * multiplier.
 *
 * @param enemies        space-separated tokens, e.g. {@code "3 s e 4 c"}
 * @param hp             base health each enemy is given, before per-type adjustment
 * @param price          bounty per kill, and the score penalty if one leaks
 * @param rank           this wave's default {@link Rank} - scales body size, and each type's own
 *                       twist (a square's resistance, a triangle's top speed, a corpse's fade
 *                       duration)
 * @param speedMultiplier this wave's own pace, composed with its path's {@code PathDefinition
 *                        .speedMultiplier()} - defaults to {@code 1x} through the 4-argument
 *                        constructor below; call {@link #withSpeedMultiplier} to call out a
 *                        specific round as faster or slower than its path's own default
 */
public record WaveDefinition(String enemies, int hp, int price, Rank rank, float speedMultiplier) {

    public WaveDefinition {
        if (speedMultiplier <= 0f) {
            throw new IllegalArgumentException("A wave's speed multiplier must be positive, was " + speedMultiplier);
        }
    }

    /**
     * Equivalent to {@code WaveDefinition(enemies, hp, price, rank, 1f)} - every pre-existing
     * four-argument construction path (this record's original shape, before a per-wave speed
     * multiplier existed) stays at today's default pace without needing to change.
     */
    public WaveDefinition(String enemies, int hp, int price, Rank rank) {
        this(enemies, hp, price, rank, 1f);
    }

    public WaveDefinition withSpeedMultiplier(float speedMultiplier) {
        return new WaveDefinition(this.enemies, this.hp, this.price, this.rank, speedMultiplier);
    }
}
