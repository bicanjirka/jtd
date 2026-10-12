package td.tower.splash;

/**
 * How a blast builds Saturation on the enemies it catches.
 *
 * @param perHit      stacks an enemy gains from a blast that catches it
 * @param innerPerHit stacks an enemy in the inner half of the blast gains instead
 * @param cap         the most stacks an enemy holds; {@code 0} when the blast builds none
 */
public record SaturationRule(int perHit, int innerPerHit, int cap) {

    private static final float INNER_HALF = 0.5f;

    public static SaturationRule none() {
        return new SaturationRule(0, 0, 0);
    }

    public boolean isActive() {
        return this.cap > 0;
    }

    /** The stacks an enemy {@code distanceShare} of the blast radius from its centre gains. */
    public int stacksAt(float distanceShare) {
        return distanceShare <= INNER_HALF ? this.innerPerHit : this.perHit;
    }
}
