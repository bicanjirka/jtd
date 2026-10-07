package td.tower.sonar;

/** Horizon: far hits grow further, but the beam deals nothing close in. */
public final class HorizonPerk implements SonarPerk {

    private static final float DEAD_ZONE_CELLS = 1.5f;
    private static final float FAR_BONUS = 1.5f;

    @Override
    public SonarSpec refineSpec(SonarSpec spec) {
        return spec.withReach(spec.reach().withDeadZone(DEAD_ZONE_CELLS * spec.view().cellSize()));
    }

    @Override
    public SonarStrike shape(SonarStrike strike, StrikeContext context) {
        return strike.withFarBonus(FAR_BONUS);
    }
}
