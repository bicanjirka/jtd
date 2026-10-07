package td.tower.pulse;

/**
 * The zap the field fires once a second: whom it hits, how hard and what it leaves behind.
 *
 * @param damageFactor   a multiple of a field tick's damage; {@code 0} for no zap
 * @param critBonus      extra crit chance on the zap
 * @param tollRaises     whether the Toll on its target raises the zap's damage too
 * @param chains         how many more enemies it jumps to, within {@code chainCells} of the last
 * @param chainCells     how far, in cells, a jump may reach, even beyond the field
 * @param dazeSeconds    how long every enemy it hits is Dazed
 */
public record ZapSpec(float damageFactor, float critBonus, boolean tollRaises, int chains, float chainCells,
                      float dazeSeconds) {

    /** No zap. */
    public static ZapSpec none() {
        return new ZapSpec(0f, 0f, false, 0, 0f, 0f);
    }

    /** A zap of {@code damageFactor} times a tick, alone on its target. */
    public static ZapSpec of(float damageFactor) {
        return new ZapSpec(damageFactor, 0f, false, 0, 0f, 0f);
    }

    public boolean isActive() {
        return this.damageFactor > 0f;
    }

    public ZapSpec withCritBonus(float bonus) {
        return new ZapSpec(this.damageFactor, this.critBonus + bonus, this.tollRaises, this.chains, this.chainCells,
                this.dazeSeconds);
    }

    public ZapSpec withTollRaising() {
        return new ZapSpec(this.damageFactor, this.critBonus, true, this.chains, this.chainCells, this.dazeSeconds);
    }

    public ZapSpec chainingTo(int chains, float chainCells) {
        return new ZapSpec(this.damageFactor, this.critBonus, this.tollRaises, chains, chainCells, this.dazeSeconds);
    }

    public ZapSpec dazing(float dazeSeconds) {
        return new ZapSpec(this.damageFactor, this.critBonus, this.tollRaises, this.chains, this.chainCells,
                dazeSeconds);
    }
}
