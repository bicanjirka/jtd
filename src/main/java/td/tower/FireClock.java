package td.tower;

import td.util.ThreadConfined;

/**
 * When a tower may shoot, kept as charge so that a fire rate that is no whole number of ticks still
 * averages out exactly. Every tick adds the tower's fire rate to the charge; one shot costs one
 * period (the base ticks between shots), and what is left over stays, so a rate of 1.1 shoots eleven
 * times in ten periods rather than rounding the wait to a whole tick.
 * <p>
 * A tower that nothing is in reach of does not bank charge past one period, and one that fires every
 * tick keeps its remainder, so only waiting is ever lost.
 */
@ThreadConfined(value = ThreadConfined.Owner.ENCLOSING)
final class FireClock {

    /** Charge within this of a whole period counts as it, which absorbs float error in a rate. */
    private static final double EPSILON = 1e-6;

    private final double period;
    private double charge;
    private boolean spentThisTick;

    /** Ready at once, as a tower that has never fired. */
    FireClock(int coolDownMax) {
        this.period = coolDownMax + 1;
        this.charge = this.period;
    }

    /** How many shots the charge pays for now; more than one only for a rate above a shot per tick. */
    int shotsDue() {
        return (int) Math.floor((this.charge + EPSILON) / this.period);
    }

    boolean isReady() {
        return this.shotsDue() >= 1;
    }

    /** Pays for one shot. */
    void spend() {
        this.spend(1.0);
    }

    /**
     * Pays {@code periods} for a shot: less than one makes the wait after it shorter, more makes it
     * longer.
     */
    void spend(double periods) {
        this.charge -= this.period * periods;
        this.spentThisTick = true;
    }

    /**
     * Ends the tick: {@code rate} more charge. A tower that was ready and did not shoot keeps at most
     * one period; one that shot keeps at most what it can spend next tick.
     */
    void advance(double rate) {
        boolean idle = !this.spentThisTick && this.isReady();
        this.charge += rate;
        if (idle) {
            this.charge = Math.min(this.charge, this.period);
        } else if (this.spentThisTick) {
            this.charge = Math.min(this.charge, this.period + rate);
        }
        this.spentThisTick = false;
    }

    /** How much of the wait is left, from {@code 1} just after a shot down to {@code 0} once it is ready. */
    float remaining() {
        return (float) Math.max(0.0, Math.min(1.0, 1.0 - this.charge / this.period));
    }
}
