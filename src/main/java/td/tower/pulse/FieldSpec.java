package td.tower.pulse;

/**
 * How hard the field hits.
 *
 * @param perDeathBonus the extra every death inside has added to the field until the wave ends, as a share of the hit
 */
public record FieldSpec(float perDeathBonus) {

    /** The base field: no bonus for deaths. */
    public static FieldSpec base() {
        return new FieldSpec(0f);
    }

    public FieldSpec withPerDeathBonus(float perDeathBonus) {
        return new FieldSpec(perDeathBonus);
    }
}
