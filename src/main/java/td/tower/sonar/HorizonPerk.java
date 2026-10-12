package td.tower.sonar;

/** Horizon: far hits grow further. */
public final class HorizonPerk implements SonarPerk {

    private static final float FAR_BONUS = 1.5f;

    @Override
    public SonarStrike shape(SonarStrike strike, StrikeContext context) {
        return strike.withFarBonus(FAR_BONUS);
    }
}
