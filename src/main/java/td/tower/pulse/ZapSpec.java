package td.tower.pulse;

/**
 * The zap the field fires once a second: whom it marks, and what it leaves on the enemies it chains to.
 *
 * @param markTicks   how long its first enemy is a Priority, and every enemy it hits Exposed; {@code 0} for no zap
 * @param chains      how many more enemies it jumps to, within {@code chainCells} of the last
 * @param chainCells  how far, in cells, a jump may reach, even beyond the field
 * @param dazeSeconds how long every enemy it hits is Dazed
 */
public record ZapSpec(int markTicks, int chains, float chainCells, float dazeSeconds) {

    /** No zap. */
    public static ZapSpec none() {
        return new ZapSpec(0, 0, 0f, 0f);
    }

    /** A zap that marks its one target for {@code markTicks}. */
    public static ZapSpec marking(int markTicks) {
        return new ZapSpec(markTicks, 0, 0f, 0f);
    }

    public boolean isActive() {
        return this.markTicks > 0;
    }

    public ZapSpec chainingTo(int chains, float chainCells) {
        return new ZapSpec(this.markTicks, chains, chainCells, this.dazeSeconds);
    }

    public ZapSpec dazing(float dazeSeconds) {
        return new ZapSpec(this.markTicks, this.chains, this.chainCells, dazeSeconds);
    }
}
