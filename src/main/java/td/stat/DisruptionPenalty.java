package td.stat;

/**
 * The summed disruption at one point: attack speed to subtract as a fraction, range in cells, and the chance in
 * {@code [0, 1]} that a hit misses. Misses combine as independent chances, so they never pass 1.
 * {@link #none()} is the identity of {@link #plus}.
 */
public record DisruptionPenalty(float attackSpeed, float rangeCells, float missChance) {

    private static final DisruptionPenalty NONE = new DisruptionPenalty(0f, 0f, 0f);

    public DisruptionPenalty(float attackSpeed, float rangeCells) {
        this(attackSpeed, rangeCells, 0f);
    }

    public static DisruptionPenalty none() {
        return NONE;
    }

    /** Nothing but a chance that every hit misses. */
    public static DisruptionPenalty blinding(float missChance) {
        return new DisruptionPenalty(0f, 0f, missChance);
    }

    public DisruptionPenalty plus(DisruptionPenalty other) {
        return new DisruptionPenalty(this.attackSpeed + other.attackSpeed, this.rangeCells + other.rangeCells,
                1f - (1f - this.missChance) * (1f - other.missChance));
    }

    public boolean isNone() {
        return this.attackSpeed == 0f && this.rangeCells == 0f && this.missChance == 0f;
    }
}
