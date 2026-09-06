package td;

/**
 * Named presets over the engine's tick-speed multiplier. 1.0 runs at the
 * baseline rate the game was tuned at; any other non-negative value - not
 * just these presets - is a valid multiplier for the tick loop, so a future
 * UI (e.g. a slider) can set an arbitrary speed without any engine change.
 */
public enum TickSpeed {
    PAUSED(0.0),
    NORMAL(1.0),
    FAST(50.0 / 15.0),
    SUPER_FAST(50.0 / 3.0);

    private final double multiplier;

    TickSpeed(double multiplier) {
        this.multiplier = multiplier;
    }

    public double multiplier() {
        return this.multiplier;
    }

    /**
     * Cycles through the playable presets (skipping PAUSED, which is reached
     * via the dedicated pause control, not by cycling).
     */
    public TickSpeed next() {
        TickSpeed[] cycle = {NORMAL, FAST, SUPER_FAST};
        for (int i = 0; i < cycle.length; i++) {
            if (cycle[i] == this) {
                return cycle[(i + 1) % cycle.length];
            }
        }
        return NORMAL;
    }
}
