package td.util;

/**
 * The simulation's tick rate, defined once, so the loop's timestep and towers' displayed fire rates
 * can't disagree. In {@code td.util} so domain packages can read it.
 */
public final class TickRate {

    /** Nanoseconds per tick at normal speed. */
    public static final long TICK_NANOS = 50_000_000L;

    /** Ticks per second at normal speed. */
    public static final float TICKS_PER_SECOND = 1_000_000_000f / TICK_NANOS;

    private TickRate() {
    }
}
