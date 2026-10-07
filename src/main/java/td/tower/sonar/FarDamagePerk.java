package td.tower.sonar;

/** Hits grow with distance, by up to a bonus at the edge of the range. */
public final class FarDamagePerk implements SonarPerk {

    private final float bonus;

    public FarDamagePerk(float bonus) {
        this.bonus = bonus;
    }

    @Override
    public SonarStrike shape(SonarStrike strike, StrikeContext context) {
        return strike.withFarBonus(this.bonus);
    }
}
