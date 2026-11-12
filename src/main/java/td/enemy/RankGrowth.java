package td.enemy;

import java.util.List;

/**
 * How one enemy's health and bounty climb its rank ladder. Health follows a curve that bends upward, so each rank
 * adds more than the one before and Elite and Boss stand out; how sharply it bends is the enemy's own. An enemy whose
 * strengths come in at their own ranks writes its health rank by rank instead. Bounty grows only with the square root
 * of health, so a tougher rank asks for more damage per credit it pays, unless it is written rank by rank too: what an
 * enemy is worth is its power, which health alone does not tell.
 *
 * @param bossHealthFactor how many times the grunt's health a Boss has; above 1
 * @param curvePower       how sharply the curve bends: 1 is a straight line, more keeps the low ranks close to the
 *                         grunt and pulls Elite and Boss away
 * @param healthSteps      the health of every rank above the grunt in ladder order, in place of the curve; empty to
 *                         follow the curve
 * @param bountySteps      the bounty of every rank above the grunt in ladder order; empty to follow health
 */
public record RankGrowth(float bossHealthFactor, float curvePower, List<Integer> healthSteps, List<Integer> bountySteps) {

    // Quadratic in a rank's place on the ladder: gentle at the bottom, so a Soldier stays close to a Grunt, and steep
    // at the top, so Elite and Boss pull away.
    private static final float STANDARD_CURVE_POWER = 2f;
    private static final int RANKS_ABOVE_GRUNT = Rank.values().length - 1;

    public RankGrowth {
        healthSteps = List.copyOf(healthSteps);
        bountySteps = List.copyOf(bountySteps);
        if (bossHealthFactor <= 1f) {
            throw new IllegalArgumentException("A Boss must have more health than a grunt, was " + bossHealthFactor);
        }
        if (curvePower <= 0f) {
            throw new IllegalArgumentException("A growth curve's power must be above zero, was " + curvePower);
        }
        requireWholeLadder(healthSteps, "health");
        requireWholeLadder(bountySteps, "bounty");
    }

    public static RankGrowth of(float bossHealthFactor) {
        return new RankGrowth(bossHealthFactor, STANDARD_CURVE_POWER, List.of(), List.of());
    }

    /** This growth on a curve that bends by {@code curvePower}. */
    public RankGrowth bending(float curvePower) {
        return new RankGrowth(this.bossHealthFactor, curvePower, this.healthSteps, this.bountySteps);
    }

    /** Health written rank by rank, whatever the grunt has. */
    public static RankGrowth stepped(int soldier, int veteran, int elite, int boss) {
        // The factor is not read once every rank is written.
        return new RankGrowth(STANDARD_CURVE_POWER, STANDARD_CURVE_POWER, List.of(soldier, veteran, elite, boss), List.of());
    }

    /** This growth with the bounty written rank by rank. */
    public RankGrowth paying(int soldier, int veteran, int elite, int boss) {
        return new RankGrowth(this.bossHealthFactor, this.curvePower, this.healthSteps,
                List.of(soldier, veteran, elite, boss));
    }

    public int healthAt(int gruntHealth, Rank rank) {
        if (rank == Rank.GRUNT || this.healthSteps.isEmpty()) {
            return (int) Math.round(gruntHealth * this.healthFactorAt(rank));
        }
        return this.healthSteps.get(rank.ordinal() - 1);
    }

    public int bountyAt(int gruntBounty, Rank rank) {
        if (rank != Rank.GRUNT && !this.bountySteps.isEmpty()) {
            return this.bountySteps.get(rank.ordinal() - 1);
        }
        if (rank != Rank.GRUNT && !this.healthSteps.isEmpty()) {
            throw new IllegalStateException("A ladder with its health written rank by rank writes its bounty too");
        }
        return (int) Math.round(gruntBounty * Math.sqrt(this.healthFactorAt(rank)));
    }

    private double healthFactorAt(Rank rank) {
        double place = rank.ordinal() / (double) Rank.BOSS.ordinal();
        return 1.0 + (this.bossHealthFactor - 1.0) * Math.pow(place, this.curvePower);
    }

    private static void requireWholeLadder(List<Integer> steps, String what) {
        if (!steps.isEmpty() && steps.size() != RANKS_ABOVE_GRUNT) {
            throw new IllegalArgumentException("A ladder's " + what + " is written for every rank above the grunt, had "
                    + steps.size());
        }
    }
}
