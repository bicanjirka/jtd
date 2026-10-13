package td.tower.sniper;

/** Unbroken Aim: a kill does not end Steady Aim; only aiming at a living enemy does. */
public final class UnbrokenAimPerk implements SniperPerk {

    @Override
    public SniperSpec refineSpec(SniperSpec spec) {
        return spec.withSteadyAim(spec.steadyAim().thatSurvivesAKill());
    }

    @Override
    public String describe() {
        return "Steady Aim survives a kill; only retargeting a living enemy resets it";
    }
}
