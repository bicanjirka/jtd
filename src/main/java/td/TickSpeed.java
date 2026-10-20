package td;

import td.util.TickRate;

import java.util.List;

/**
 * The tick-speed multiplier, with named presets. 1.0 is the baseline rate; any non-negative
 * multiplier is valid, so the dev panel can dial in whole ticks per second between the presets.
 */
public record TickSpeed(double multiplier) {

    public static final TickSpeed PAUSED = new TickSpeed(0.0);
    public static final TickSpeed NORMAL = new TickSpeed(1.0);
    public static final TickSpeed FAST = new TickSpeed(50.0 / 15.0);
    public static final TickSpeed SUPER_FAST = new TickSpeed(50.0 / 3.0);

    private static final List<TickSpeed> PLAYABLE = List.of(NORMAL, FAST, SUPER_FAST);

    public TickSpeed {
        if (multiplier < 0.0 || !Double.isFinite(multiplier)) {
            throw new IllegalArgumentException("Tick speed must be finite and non-negative: " + multiplier);
        }
    }

    public static TickSpeed ofTicksPerSecond(int ticksPerSecond) {
        return new TickSpeed(ticksPerSecond / (double) TickRate.TICKS_PER_SECOND);
    }

    /** Ticks per second of wall-clock time, rounded to whole ticks. */
    public int ticksPerSecond() {
        return (int) Math.round(this.multiplier * TickRate.TICKS_PER_SECOND);
    }

    public boolean isPaused() {
        return this.multiplier == 0.0;
    }

    /** {@code delta} whole ticks per second faster than the rounded rate, never below paused. */
    public TickSpeed nudged(int delta) {
        return ofTicksPerSecond(Math.max(0, this.ticksPerSecond() + delta));
    }

    /** The next playable preset faster than this one, wrapping round to {@code NORMAL}. */
    public TickSpeed next() {
        return PLAYABLE.stream().filter(preset -> preset.multiplier > this.multiplier).findFirst().orElse(NORMAL);
    }
}
