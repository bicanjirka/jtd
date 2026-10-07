package td.tower.sniper;

/** Steady Aim builds to more stacks. */
public final class SteadyAimStacksPerk implements SniperPerk {

    private final int stackCap;

    public SteadyAimStacksPerk(int stackCap) {
        this.stackCap = stackCap;
    }

    @Override
    public SniperSpec refineSpec(SniperSpec spec) {
        return spec.withSteadyAim(spec.steadyAim().withStackCap(this.stackCap));
    }
}
