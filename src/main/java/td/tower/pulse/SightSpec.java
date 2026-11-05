package td.tower.pulse;

/**
 * What the field shows of the enemies inside.
 *
 * @param revealTicks how long an enemy stays revealed after its last tick inside; {@code 0} for no reveal
 */
public record SightSpec(int revealTicks) {

    /** The base field shows nothing. */
    public static SightSpec none() {
        return new SightSpec(0);
    }

    public SightSpec revealing(int revealTicks) {
        return new SightSpec(revealTicks);
    }

    public boolean reveals() {
        return this.revealTicks > 0;
    }
}
