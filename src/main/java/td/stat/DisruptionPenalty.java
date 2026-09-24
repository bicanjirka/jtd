package td.stat;

/**
 * The summed disruption at one point, as fractions to subtract. {@link #none()} is the identity of
 * {@link #plus}.
 */
public record DisruptionPenalty(float fireRate, float range) {

    private static final DisruptionPenalty NONE = new DisruptionPenalty(0f, 0f);

    public static DisruptionPenalty none() {
        return NONE;
    }

    public DisruptionPenalty plus(DisruptionPenalty other) {
        return new DisruptionPenalty(this.fireRate + other.fireRate, this.range + other.range);
    }

    public boolean isNone() {
        return this.fireRate == 0f && this.range == 0f;
    }
}
