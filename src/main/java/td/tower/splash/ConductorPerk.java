package td.tower.splash;

/** Conductor: arcs can crit, and jump once more. */
public final class ConductorPerk implements SplashPerk {

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withArcs(spec.arcs().thatCrit().withMoreJumps(1));
    }
}
