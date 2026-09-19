package td.wave;

import td.enemy.Rank;

/**
 * One authored wave: its contents as a token string in the wave mini-language (see the table
 * in CLAUDE.md), the {@link Rank} every slot in it defaults to, and its own optional speed
 * multiplier. Health and bounty are no longer authored here - they live on each spawned enemy's
 * own {@code EnemyDefinition}, scoped per rank (see {@code td.enemy.RankedEnemy}), so a rank-1
 * Circle has the same health and bounty in every wave that spawns it.
 *
 * @param enemies        space-separated tokens, e.g. {@code "3 s e 4 c"}
 * @param rank           this wave's default {@link Rank} - any slot may override it inline (see
 *                       {@code td.wave.WaveScript})
 * @param speedMultiplier this wave's own pace, composed with its path's {@code PathDefinition
 *                        .speedMultiplier()} - defaults to {@code 1x} through the 2-argument
 *                        constructor below; call {@link #withSpeedMultiplier} to call out a
 *                        specific round as faster or slower than its path's own default
 */
public record WaveDefinition(String enemies, Rank rank, float speedMultiplier) {

    public WaveDefinition {
        if (speedMultiplier <= 0f) {
            throw new IllegalArgumentException("A wave's speed multiplier must be positive, was " + speedMultiplier);
        }
    }

    /**
     * Equivalent to {@code WaveDefinition(enemies, rank, 1f)} - every pre-existing two-argument
     * construction path stays at today's default pace without needing to change.
     */
    public WaveDefinition(String enemies, Rank rank) {
        this(enemies, rank, 1f);
    }

    public WaveDefinition withSpeedMultiplier(float speedMultiplier) {
        return new WaveDefinition(this.enemies, this.rank, speedMultiplier);
    }
}
