package td.tower.seeker;

import td.effect.EffectKind;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;
import td.util.TickRate;

/** A Release: every so many freezes set off a Swarm or a Hatching, as many missiles as the nest holds. */
public final class ReleaseMechanic implements SeekerMechanic {

    /** Freezes that set off one Release; every freeze counts, a released missile's too. */
    public static final int EVERY = 11;
    /** Ticks between the missiles a Hatching hatches. */
    public static final int HATCH_GAP_TICKS = 8;

    private final Release release;

    public ReleaseMechanic(Release release) {
        if (release == Release.NONE) {
            throw new IllegalArgumentException("a Release mechanic releases something");
        }
        this.release = release;
    }

    @Override
    public SeekerSpec refineSpec(SeekerSpec spec) {
        return spec.withRelease(this.release);
    }

    @Override
    public TermText describe() {
        TermText every = TermText.of("every " + TermNumbers.ordinal(EVERY) + " ").plus(EffectKind.FREEZE);
        return switch (this.release) {
            case NONE -> TermText.empty();
            case SWARM -> every.plus(" releases a ").plus(TowerTerm.SWARM).plus(": as many missiles as the ")
                    .plus(TowerTerm.NEST).plus(" holds, at once, one at each enemy in range, soonest out first, and round again "
                    + "while missiles are left");
            case HATCHING -> every.plus(" sets off a ").plus(TowerTerm.HATCHING).plus(": as many missiles as the ")
                    .plus(TowerTerm.NEST).plus(" holds hatch on top of it, one every "
                    + TermNumbers.seconds(HATCH_GAP_TICKS / TickRate.TICKS_PER_SECOND)
                    + ", and launch first while it keeps loading");
        };
    }
}
