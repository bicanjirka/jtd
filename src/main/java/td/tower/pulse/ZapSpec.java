package td.tower.pulse;

/**
 * The zap the field fires once a second at the visible enemy of the highest rank inside.
 *
 * @param markTicks how long, after the last zap, it stays every tower's Priority and Condemned; {@code 0} for no zap
 */
public record ZapSpec(int markTicks) {

    /** No zap. */
    public static ZapSpec none() {
        return new ZapSpec(0);
    }

    /** A zap that marks its target for {@code markTicks}. */
    public static ZapSpec marking(int markTicks) {
        return new ZapSpec(markTicks);
    }

    public boolean isActive() {
        return this.markTicks > 0;
    }
}
