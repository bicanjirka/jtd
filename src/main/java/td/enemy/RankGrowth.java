package td.enemy;

/**
 * How one enemy's health and bounty climb its rank ladder, for a ladder that derives them instead of writing each
 * rank's integers: health multiplies by {@code healthFactor} each rank, bounty adds {@code bountyStep} once for
 * Soldier and Veteran, three more times for Elite and five more for Boss.
 *
 * @param healthFactor what each rank multiplies the previous rank's health by
 * @param bountyStep   what one bounty step adds to the grunt's bounty
 */
public record RankGrowth(float healthFactor, int bountyStep) {

    // Elite climbs three bounty steps and Boss five more, so the ranks that need a plan pay for one.
    private static final int[] BOUNTY_STEPS = {0, 1, 2, 5, 10};

    public static RankGrowth of(float healthFactor, int bountyStep) {
        return new RankGrowth(healthFactor, bountyStep);
    }

    public int healthAt(int gruntHealth, Rank rank) {
        return Math.round(gruntHealth * (float) Math.pow(this.healthFactor, rank.ordinal()));
    }

    public int bountyAt(int gruntBounty, Rank rank) {
        return gruntBounty + this.bountyStep * BOUNTY_STEPS[rank.ordinal()];
    }
}
