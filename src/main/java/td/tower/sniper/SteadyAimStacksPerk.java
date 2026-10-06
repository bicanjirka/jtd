package td.tower.sniper;

/** Steady Aim builds to more stacks. */
public final class SteadyAimStacksPerk implements SniperPerk {

    private final int stackCap;

    public SteadyAimStacksPerk(int stackCap) {
        this.stackCap = stackCap;
    }

    @Override
    public AimRules refineAim(AimRules rules) {
        return rules.withStackCap(this.stackCap);
    }
}
