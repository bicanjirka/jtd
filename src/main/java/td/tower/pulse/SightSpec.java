package td.tower.pulse;

/**
 * What the field shows of the enemies inside.
 *
 * @param revealTicks how long an enemy stays revealed after its last tick inside; {@code 0} for no reveal
 * @param unveils     whether a revealed enemy can't be shrouded or fade while the reveal lasts
 */
public record SightSpec(int revealTicks, boolean unveils) {

    /** The base field shows nothing. */
    public static SightSpec none() {
        return new SightSpec(0, false);
    }

    public SightSpec revealing(int revealTicks) {
        return new SightSpec(revealTicks, this.unveils);
    }

    public SightSpec unveiling() {
        return new SightSpec(this.revealTicks, true);
    }

    public boolean reveals() {
        return this.revealTicks > 0;
    }
}
