package td.tower.seeker;

/**
 * When and how hard a frozen enemy shatters, hurting what stands near it.
 *
 * @param onKill     whether a frozen enemy the Seeker kills shatters
 * @param multiplier a multiple of the base shatter
 * @param onThaw     whether an enemy the Seeker froze shatters, without dying, when its freeze ends
 */
public record ShatterSpec(boolean onKill, float multiplier, boolean onThaw) {

    /** No shatter. */
    public static ShatterSpec none() {
        return new ShatterSpec(false, 1f, false);
    }

    public ShatterSpec alsoOnKill() {
        return new ShatterSpec(true, this.multiplier, this.onThaw);
    }

    /** This shatter {@code factor} times as hard. */
    public ShatterSpec harderBy(float factor) {
        return new ShatterSpec(this.onKill, this.multiplier * factor, this.onThaw);
    }

    public ShatterSpec alsoOnThaw() {
        return new ShatterSpec(this.onKill, this.multiplier, true);
    }
}
