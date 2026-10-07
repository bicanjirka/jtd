package td.tower.pulse;

/**
 * How hard the field hits.
 *
 * @param fullTollBonus the extra a field hit deals to an enemy at full Toll, as a share of the hit
 */
public record FieldSpec(float fullTollBonus) {

    /** The base field: no bonus at full Toll. */
    public static FieldSpec base() {
        return new FieldSpec(0f);
    }

    public FieldSpec withFullTollBonus(float fullTollBonus) {
        return new FieldSpec(fullTollBonus);
    }
}
