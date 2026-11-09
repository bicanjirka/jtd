package td.wave;

/**
 * One authored wave. Health and bounty come from each enemy's definition at its rank.
 *
 * @param enemies         the wave script, e.g. {@code "soldier: 3 knight e 4 walker"}
 * @param clearBonus      credits paid when the round this wave belongs to clears with no enemy leaked; none by
 *                        default
 */
public record WaveDefinition(String enemies, int clearBonus) {

    public WaveDefinition {
        if (clearBonus < 0) {
            throw new IllegalArgumentException("A wave's clear bonus must not be negative, was " + clearBonus);
        }
    }

    /** With no clear bonus. */
    public WaveDefinition(String enemies) {
        this(enemies, 0);
    }

    public WaveDefinition withClearBonus(int clearBonus) {
        return new WaveDefinition(this.enemies, clearBonus);
    }
}
