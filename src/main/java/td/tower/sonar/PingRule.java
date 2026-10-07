package td.tower.sonar;

/**
 * Whom a revolution Exposes: the healthiest enemies the beam passed, and for how long.
 *
 * @param count         how many it Exposes; {@code 0} for no ping at all
 * @param minRangeShare how far out an enemy must be to count, as a share of the range
 * @param passes        how many revolutions the Exposed lasts
 */
public record PingRule(int count, float minRangeShare, int passes) {

    public static PingRule none() {
        return new PingRule(0, 0f, 1);
    }

    public boolean isActive() {
        return this.count > 0;
    }

    public PingRule withCount(int count) {
        return new PingRule(count, this.minRangeShare, this.passes);
    }

    public PingRule withMinRangeShare(float minRangeShare) {
        return new PingRule(this.count, minRangeShare, this.passes);
    }

    public PingRule withPasses(int passes) {
        return new PingRule(this.count, this.minRangeShare, passes);
    }
}
