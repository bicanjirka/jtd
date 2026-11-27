package td.tower.cinder;

/**
 * A band of the Scorcher's cone: the middle {@code share} of its width, out to its full reach, where it pours
 * {@code more} of the Flame on top of what the cone pours. Bands nest and add: an enemy in two bands takes both.
 */
public record FlameBand(float share, float more) {

    private static final FlameBand NONE = new FlameBand(0f, 0f);

    /** No band: nothing pours more. */
    public static FlameBand none() {
        return NONE;
    }

    public boolean isActive() {
        return this.share > 0f && this.more != 0f;
    }

    /**
     * What it adds to the Flame of an enemy {@code offAxis} radians off the heading of a cone {@code halfWidth} radians
     * wide either side: its {@code more} when it holds the enemy, edge included, else nothing.
     */
    public float moreAt(double offAxis, double halfWidth) {
        return this.isActive() && offAxis <= halfWidth * this.share ? this.more : 0f;
    }

    /** Its half-width in a cone {@code halfWidth} wide either side, 0 when it is no band. */
    public float halfWidthIn(float halfWidth) {
        return this.isActive() ? halfWidth * this.share : 0f;
    }
}
