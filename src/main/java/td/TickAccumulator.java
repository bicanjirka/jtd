package td;


import td.util.ThreadConfined;

/**
 * Converts elapsed nanoseconds into whole logic ticks, carrying the remainder to the next call.
 * Clock-free, so the tick math is testable with fabricated times.
 */
@ThreadConfined(value = ThreadConfined.Owner.ENCLOSING)
public final class TickAccumulator {

    private final long stepNanos;
    private long accumulatedNanos;

    public TickAccumulator(long stepNanos) {
        if (stepNanos <= 0) {
            throw new IllegalArgumentException("stepNanos must be positive: " + stepNanos);
        }
        this.stepNanos = stepNanos;
    }

    /** Adds elapsed time and returns how many whole ticks it now covers, keeping the remainder. */
    public int accumulate(long elapsedNanos) {
        if (elapsedNanos < 0) {
            throw new IllegalArgumentException("elapsedNanos must not be negative: " + elapsedNanos);
        }
        this.accumulatedNanos += elapsedNanos;
        int ticks = (int) (this.accumulatedNanos / this.stepNanos);
        this.accumulatedNanos -= (long) ticks * this.stepNanos;
        return ticks;
    }

    public void reset() {
        this.accumulatedNanos = 0;
    }

    /** The remainder as a fraction of one step, in {@code [0, 1)}. */
    public double fractionElapsed() {
        return this.accumulatedNanos / (double) this.stepNanos;
    }
}
