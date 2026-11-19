package td.tower.splash;

import td.term.TermText;
import td.tower.TowerTerm;

/** Chain Lightning: the Arc jumps more times. */
public final class ChainLightningMechanic implements SplashMechanic {

    private static final int MORE_JUMPS = 5;

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withArc(spec.arc().withMoreJumps(MORE_JUMPS));
    }

    @Override
    public TermText describe() {
        return TermText.of(TowerTerm.ARC).plus(" jumps " + MORE_JUMPS + " more times");
    }
}
