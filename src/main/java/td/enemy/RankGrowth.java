package td.enemy;

/**
 * How one enemy's health and bounty climb its rank ladder. Health follows a curve that bends upward, so each rank
 * adds more than the one before and Elite and Boss stand out; bounty grows only with the square root of health, so a
 * tougher rank asks for more damage per credit it pays.
 *
 * @param bossHealthFactor how many times the grunt's health a Boss has; above 1
 */
public record RankGrowth(float bossHealthFactor) {

    // Quadratic in a rank's place on the ladder: gentle at the bottom, so a Soldier stays close to a Grunt, and steep
    // at the top, so Elite and Boss pull away.
    private static final double HEALTH_CURVE_POWER = 2.0;

    public RankGrowth {
        if (bossHealthFactor <= 1f) {
            throw new IllegalArgumentException("A Boss must have more health than a grunt, was " + bossHealthFactor);
        }
    }

    public static RankGrowth of(float bossHealthFactor) {
        return new RankGrowth(bossHealthFactor);
    }

    public int healthAt(int gruntHealth, Rank rank) {
        return (int) Math.round(gruntHealth * this.healthFactorAt(rank));
    }

    public int bountyAt(int gruntBounty, Rank rank) {
        return (int) Math.round(gruntBounty * Math.sqrt(this.healthFactorAt(rank)));
    }

    private double healthFactorAt(Rank rank) {
        double place = rank.ordinal() / (double) Rank.BOSS.ordinal();
        return 1.0 + (this.bossHealthFactor - 1.0) * Math.pow(place, HEALTH_CURVE_POWER);
    }
}
