package td.tower.seeker;

import td.effect.EffectKind;
import td.term.TermNumbers;
import td.term.TermText;
import td.util.TickRate;

/** Feeding: each freeze takes time off the next load, never below half its time. */
public final class FeedingMechanic implements SeekerMechanic {

    /** Ticks a freeze takes off the load, at the tower's own pace. */
    public static final int FED_TICKS = 5;
    /** The least share of its time a load may take, however many freezes feed it. */
    public static final double FLOOR = 0.5;

    /**
     * The charge one freeze adds to a load of {@code period} at {@code rate}, when freezes have fed it {@code fed}
     * already: never so much that the load takes less than its floor.
     */
    public static double feedFor(double fed, double period, double rate) {
        return Math.max(0.0, Math.min(FED_TICKS * rate, period * (1.0 - FLOOR) - fed));
    }

    @Override
    public SeekerSpec refineSpec(SeekerSpec spec) {
        return spec.feeding();
    }

    @Override
    public TermText describe() {
        return TermText.of("each ").plus(EffectKind.FREEZE).plus(" takes "
                + TermNumbers.seconds(FED_TICKS / TickRate.TICKS_PER_SECOND) + " off the next load, never below "
                + TermNumbers.percent((float) FLOOR) + " of its time");
    }
}
