package td.tower.splash;

/** Chain Lightning: at least six jumps at the blast's full damage, and the third jump forks. */
public final class ChainLightningPerk implements SplashPerk {

    private static final int JUMPS = 6;

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withArcs(spec.arcs().withMinimumJumps(JUMPS).withShareAtLeast(1f).thatForks());
    }
}
