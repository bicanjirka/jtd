package td.tower.pulse;

/**
 * The zap the field fires once a second at the healthiest visible enemy inside.
 *
 * @param markTicks how long it is every tower's Priority and Exposed at full field strength, shorter below it and longer
 *                  above; {@code 0} for no zap
 */
public record ZapSpec(int markTicks) {

    /** No zap. */
    public static ZapSpec none() {
        return new ZapSpec(0);
    }

    /** A zap that marks its target for {@code markTicks} at full field strength. */
    public static ZapSpec marking(int markTicks) {
        return new ZapSpec(markTicks);
    }

    public boolean isActive() {
        return this.markTicks > 0;
    }
}
