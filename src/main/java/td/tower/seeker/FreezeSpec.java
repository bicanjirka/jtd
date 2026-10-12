package td.tower.seeker;

/**
 * How a missile freezes beyond how long, which is a tower stat: how wide, and whether what it freezes
 * turns brittle.
 *
 * @param areaCells      the radius, in cells, around the impact that freezes too; {@code 0} for the target only
 * @param brittle        whether a frozen enemy takes extra physical damage
 */
public record FreezeSpec(float areaCells, boolean brittle) {

    /** The base freeze: the target only, with no brittleness. */
    public static FreezeSpec base() {
        return new FreezeSpec(0f, false);
    }

    public FreezeSpec withArea(float areaCells) {
        return new FreezeSpec(areaCells, this.brittle);
    }

    public FreezeSpec makingBrittle() {
        return new FreezeSpec(this.areaCells, true);
    }
}
