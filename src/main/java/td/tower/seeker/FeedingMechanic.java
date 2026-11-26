package td.tower.seeker;

import td.effect.EffectKind;
import td.term.TermNumbers;
import td.term.TermText;
import td.util.TickRate;

/** Feeding: each freeze takes time off the next load, however many fed it already. */
public final class FeedingMechanic implements SeekerMechanic {

    /** Ticks a freeze takes off the load, at the tower's own pace. */
    public static final int FED_TICKS = 5;
    /** The charge one freeze adds to the load at {@code rate}: its ticks at the tower's pace. */
    public static double feedFor(double rate) {
        return FED_TICKS * rate;
    }

    @Override
    public SeekerSpec refineSpec(SeekerSpec spec) {
        return spec.feeding();
    }

    @Override
    public TermText describe() {
        return TermText.of("each ").plus(EffectKind.FREEZE).plus(" takes "
                + TermNumbers.seconds(FED_TICKS / TickRate.TICKS_PER_SECOND) + " off the next load");
    }
}
