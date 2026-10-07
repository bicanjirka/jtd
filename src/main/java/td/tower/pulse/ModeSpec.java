package td.tower.pulse;

/**
 * The rules that hold while an enemy is inside the field.
 *
 * @param silences whether every enemy inside is Silenced
 * @param deadZone whether nothing inside can be healed or shielded
 */
public record ModeSpec(boolean silences, boolean deadZone) {

    /** No rules beyond the damage. */
    public static ModeSpec none() {
        return new ModeSpec(false, false);
    }

    public ModeSpec silencing() {
        return new ModeSpec(true, this.deadZone);
    }

    public ModeSpec withDeadZone() {
        return new ModeSpec(this.silences, true);
    }
}
