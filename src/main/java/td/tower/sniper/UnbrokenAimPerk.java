package td.tower.sniper;

/** Unbroken Aim: Steady Aim builds higher and a kill does not end it; only aiming at a living enemy does. */
public final class UnbrokenAimPerk implements SniperPerk {

    private static final int STACK_CAP = 5;

    @Override
    public AimRules refineAim(AimRules rules) {
        return rules.withStackCap(STACK_CAP).thatSurvivesAKill();
    }
}
