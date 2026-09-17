package td.util;

/**
 * How fast the simulation runs, in one place.
 * <p>
 * {@code GameLoop} paces its fixed timestep from {@link #TICK_NANOS}, and towers express their
 * cadence in seconds for the player's benefit and convert with {@link #TICKS_PER_SECOND}.
 * Those were once two independent literals in two packages - 50_000_000L in the loop and a
 * hand-written 20f in AbstractTower - tied together by a comment. Changing the tick rate would
 * have left every tower's displayed fire rate quietly lying about itself.
 * <p>
 * This lives in {@code td.util} rather than beside {@code GameLoop} so the domain packages can
 * read it without depending on the application root.
 */
public final class TickRate {

    /**
     * Real-world nanoseconds one simulation tick represents at {@code TickSpeed.NORMAL}.
     */
    public static final long TICK_NANOS = 50_000_000L;

    /**
     * Simulation ticks per real-world second at {@code TickSpeed.NORMAL}.
     */
    public static final float TICKS_PER_SECOND = 1_000_000_000f / TICK_NANOS;

    private TickRate() {
    }
}
