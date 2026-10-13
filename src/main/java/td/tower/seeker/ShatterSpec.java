package td.tower.seeker;

/**
 * When and how hard a frozen enemy shatters, hurting what stands near it.
 *
 * @param onKill whether a frozen enemy the Seeker kills shatters
 * @param share  the share of the shattered enemy's damage the shatter deals around it
 * @param onThaw whether an enemy the Seeker froze shatters, without dying, when its freeze ends
 */
public record ShatterSpec(boolean onKill, float share, boolean onThaw) {

    /** No shatter. */
    public static ShatterSpec none() {
        return new ShatterSpec(false, 0f, false);
    }

    /** Shatters on a kill, for {@code share} of the shattered enemy's damage. */
    public ShatterSpec alsoOnKill(float share) {
        return new ShatterSpec(true, share, this.onThaw);
    }

    /** This shatter {@code factor} times as hard. */
    public ShatterSpec harderBy(float factor) {
        return new ShatterSpec(this.onKill, this.share * factor, this.onThaw);
    }

    public ShatterSpec alsoOnThaw() {
        return new ShatterSpec(this.onKill, this.share, true);
    }
}
