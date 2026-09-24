package td;

/**
 * Named presets over the tick-speed multiplier. 1.0 is the baseline rate; any non-negative
 * multiplier is valid.
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

    /** Cycles the playable presets, skipping {@code PAUSED}. */
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
