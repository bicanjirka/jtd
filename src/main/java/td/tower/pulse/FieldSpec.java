package td.tower.pulse;

/**
 * How the field grows within a wave.
 *
 * @param perDeathGrowth how much every death inside widens the field until the wave ends, as a share of its range
 * @param maxDeathGrowth the most the deaths widen it, as a share of its range
 */
public record FieldSpec(float perDeathGrowth, float maxDeathGrowth) {

    /** The base field: it never grows. */
    public static FieldSpec base() {
        return new FieldSpec(0f, 0f);
    }

    public FieldSpec withPerDeathGrowth(float perDeathGrowth, float maxDeathGrowth) {
        return new FieldSpec(perDeathGrowth, maxDeathGrowth);
    }

    /** The share of its range the field has grown by after {@code deaths} deaths inside this wave. */
    public float growthAfter(int deaths) {
        return Math.min(this.maxDeathGrowth, this.perDeathGrowth * deaths);
    }
}
