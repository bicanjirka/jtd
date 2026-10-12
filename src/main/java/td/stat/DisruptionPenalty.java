package td.stat;

/**
 * The summed disruption at one point: attack speed to subtract as a fraction, range in cells.
 * {@link #none()} is the identity of {@link #plus}.
 */
public record DisruptionPenalty(float attackSpeed, float rangeCells) {

    private static final DisruptionPenalty NONE = new DisruptionPenalty(0f, 0f);

    public static DisruptionPenalty none() {
        return NONE;
    }

    public DisruptionPenalty plus(DisruptionPenalty other) {
        return new DisruptionPenalty(this.attackSpeed + other.attackSpeed, this.rangeCells + other.rangeCells);
    }

    public boolean isNone() {
        return this.attackSpeed == 0f && this.rangeCells == 0f;
    }
}
