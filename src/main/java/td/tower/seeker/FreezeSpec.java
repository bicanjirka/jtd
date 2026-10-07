package td.tower.seeker;

/**
 * How a missile freezes: how long, how wide, and whether what it freezes turns brittle.
 *
 * @param durationFactor a multiple of the base freeze time
 * @param areaCells      the radius, in cells, around the impact that freezes too; {@code 0} for the target only
 * @param brittle        whether a frozen enemy takes extra physical damage
 */
public record FreezeSpec(float durationFactor, float areaCells, boolean brittle) {

    /** The base freeze: the target only, for the base time, with no brittleness. */
    public static FreezeSpec base() {
        return new FreezeSpec(1f, 0f, false);
    }

    /** This freeze lasting {@code factor} times as long. */
    public FreezeSpec lastingLonger(float factor) {
        return new FreezeSpec(this.durationFactor * factor, this.areaCells, this.brittle);
    }

    public FreezeSpec withArea(float areaCells) {
        return new FreezeSpec(this.durationFactor, areaCells, this.brittle);
    }

    public FreezeSpec makingBrittle() {
        return new FreezeSpec(this.durationFactor, this.areaCells, true);
    }
}
