package td.tower.seeker;

import td.effect.EffectKind;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;
import td.util.TickRate;

/** A Release: every so many freezes set off a Swarm or a Hatching, fewer missiles than the freezes it took. */
public final class ReleaseMechanic implements SeekerMechanic {

    /** Freezes that set off one Release; every freeze counts, a released missile's too. */
    public static final int EVERY = 14;
    /** Missiles one Release sends. */
    public static final int MISSILES = 7;
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
            case SWARM -> every.plus(" releases a ").plus(TowerTerm.SWARM).plus(": " + MISSILES
                    + " missiles at once, one at each of the " + MISSILES + " soonest-out enemies in range");
            case HATCHING -> every.plus(" sets off a ").plus(TowerTerm.HATCHING).plus(": " + MISSILES
                    + " missiles hatch into the ").plus(TowerTerm.NEST).plus(" past its size, one every "
                    + TermNumbers.seconds(HATCH_GAP_TICKS / TickRate.TICKS_PER_SECOND) + ", and launch as usual");
        };
    }
}
