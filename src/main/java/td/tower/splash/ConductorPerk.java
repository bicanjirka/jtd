package td.tower.splash;

/** Conductor: arcs can crit, and jump once more. */
public final class ConductorPerk implements SplashPerk {

    private static final int MORE_JUMPS = 1;

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withArcs(spec.arcs().thatCrit().withMoreJumps(MORE_JUMPS));
    }

    @Override
    public String describe() {
        return "arcs can crit, +" + MORE_JUMPS + " jump";
    }
}
