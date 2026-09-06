package td;

/**
 * Fixed-timestep accumulator: converts elapsed wall-clock nanoseconds into a
 * whole number of logic ticks, carrying any leftover fraction of a step
 * forward to the next call. Has no thread, clock, or Swing dependency, so
 * the tick-rate math is fully unit-testable with fabricated elapsed times,
 * independent of however the real loop drives it.
 */
public final class TickAccumulator {

    private final long stepNanos;
    private long accumulatedNanos;

    public TickAccumulator(long stepNanos) {
        if (stepNanos <= 0) {
            throw new IllegalArgumentException("stepNanos must be positive: " + stepNanos);
        }
        this.stepNanos = stepNanos;
    }

    /**
     * Adds the given elapsed time to the accumulator and returns how many
     * whole logic ticks that amount of time now covers, deducting them from
     * the accumulator. Any remainder smaller than one step is kept for the
     * next call.
     */
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
}
