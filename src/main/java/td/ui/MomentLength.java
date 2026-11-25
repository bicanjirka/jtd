package td.ui;

import td.util.TickRate;

/**
 * The lengths a moment on the board may last: a shot, a hit, a blast, a rise. A moment picks the nearest step instead
 * of a length of its own, so moments of a kind share one rhythm. Steps are seconds, so a moment lasts as long at 1x
 * whichever clock runs it.
 */
enum MomentLength {

    FLICKER(0.15),
    BLINK(0.2),
    QUICK(0.3),
    SHORT(0.4),
    BEAT(0.5),
    LONG(0.8),
    SLOW(1.2),
    LINGER(3.0);

    private final double seconds;

    MomentLength(double seconds) {
        this.seconds = seconds;
    }

    double seconds() {
        return this.seconds;
    }

    /** The step on the tick clock: at least one tick. */
    int ticks() {
        return Math.max(1, (int) Math.round(this.seconds * TickRate.TICKS_PER_SECOND));
    }
}
