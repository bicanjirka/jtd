package td.tower.splash;

import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

/** Chain Lightning: at least six jumps at the blast's full damage, and the third jump forks. */
public final class ChainLightningPerk implements SplashPerk {

    private static final int JUMPS = 6;

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withArcs(spec.arcs().withMinimumJumps(JUMPS).withShareAtLeast(1f).thatForks());
    }

    @Override
    public TermText describe() {
        return TermText.of("an ").plus(TowerTerm.ARC).plus(" makes up to " + JUMPS
                + " jumps at the blast's full damage; the " + TermNumbers.ordinal(ArcPlanner.FORK_JUMP) + " jump forks");
    }
}
