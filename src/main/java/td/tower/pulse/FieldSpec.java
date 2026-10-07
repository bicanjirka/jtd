package td.tower.pulse;

/**
 * How hard the field hits.
 *
 * @param fullTollBonus the extra a field hit deals to an enemy at full Toll, as a share of the hit
 * @param perDeathBonus the extra every death inside has added to the field until the wave ends, as a share of the hit
 */
public record FieldSpec(float fullTollBonus, float perDeathBonus) {

    /** The base field: no bonus at full Toll and none for deaths. */
    public static FieldSpec base() {
        return new FieldSpec(0f, 0f);
    }

    public FieldSpec withFullTollBonus(float fullTollBonus) {
        return new FieldSpec(fullTollBonus, this.perDeathBonus);
    }

    public FieldSpec withPerDeathBonus(float perDeathBonus) {
        return new FieldSpec(this.fullTollBonus, perDeathBonus);
    }
}
