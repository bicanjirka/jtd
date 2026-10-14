package td.enemy;

/**
 * How one enemy's health and bounty climb its rank ladder: health multiplies by {@code healthFactor} each rank,
 * bounty adds {@code bountyStep} for each of the rank's {@link Rank#bountySteps() steps}.
 *
 * @param healthFactor what each rank multiplies the previous rank's health by
 * @param bountyStep   what one bounty step adds to the grunt's bounty
 */
public record RankGrowth(float healthFactor, int bountyStep) {

    public static RankGrowth of(float healthFactor, int bountyStep) {
        return new RankGrowth(healthFactor, bountyStep);
    }

    public int healthAt(int gruntHealth, Rank rank) {
        return Math.round(gruntHealth * (float) Math.pow(this.healthFactor, rank.ordinal()));
    }

    public int bountyAt(int gruntBounty, Rank rank) {
        return gruntBounty + this.bountyStep * rank.bountySteps();
    }
}
