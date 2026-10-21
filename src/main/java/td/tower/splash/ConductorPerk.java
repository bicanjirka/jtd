package td.tower.splash;

import td.term.TermText;
import td.tower.TowerTerm;

/** Conductor: arcs can crit, and jump once more. */
public final class ConductorPerk implements SplashPerk {

    private static final int MORE_JUMPS = 1;

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withArcs(spec.arcs().thatCrit().withMoreJumps(MORE_JUMPS));
    }

    @Override
    public TermText describe() {
        return TermText.of("an ").plus(TowerTerm.ARC).plus(" can crit, and makes " + MORE_JUMPS + " more jump");
    }
}
