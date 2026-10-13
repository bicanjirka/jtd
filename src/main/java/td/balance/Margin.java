package td.balance;

/**
 * How much tougher the enemies could be before a player stops holding a level: the highest enemy health, as a
 * percentage of what the level authors, at which the player still wins, found by bisection for one seed. Unlike a
 * win rate it does not saturate at 100% or 0%, so it tells an easy level from a trivial one and a hard level from
 * an impossible one.
 */
public final class Margin {

    static final int LOWEST_PERCENT = 10;
    static final int AUTHORED_PERCENT = 100;
    static final int HIGHEST_PERCENT = 1000;
    private static final int STEPS = 7;

    private Margin() {
    }

    /** The highest enemy health percentage at which {@code player} wins {@code seed}, to within a few percent. */
    public static int healthPercent(BoardSurvey survey, PlayerSpec player, long seed, LevelPlay.Pacing pacing) {
        return healthPercent(survey, player, seed, pacing, Rules.standard());
    }

    /** {@link #healthPercent(BoardSurvey, PlayerSpec, long, LevelPlay.Pacing)} on top of other rule changes. */
    public static int healthPercent(BoardSurvey survey, PlayerSpec player, long seed, LevelPlay.Pacing pacing,
                                    Rules rules) {
        // Searched outward from the level as authored: a player can lose a far-off setting for reasons of its
        // own, and those should not decide the margin near the one that matters.
        double low;
        double high;
        if (wins(survey, player, seed, pacing, rules, AUTHORED_PERCENT)) {
            if (wins(survey, player, seed, pacing, rules, HIGHEST_PERCENT)) {
                return HIGHEST_PERCENT;
            }
            low = Math.log(AUTHORED_PERCENT);
            high = Math.log(HIGHEST_PERCENT);
        } else {
            if (!wins(survey, player, seed, pacing, rules, LOWEST_PERCENT)) {
                return 0;
            }
            low = Math.log(LOWEST_PERCENT);
            high = Math.log(AUTHORED_PERCENT);
        }
        for (int step = 0; step < STEPS; step++) {
            double middle = (low + high) / 2;
            if (wins(survey, player, seed, pacing, rules, (int) Math.round(Math.exp(middle)))) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return (int) Math.round(Math.exp(low));
    }

    private static boolean wins(BoardSurvey survey, PlayerSpec player, long seed, LevelPlay.Pacing pacing,
                                Rules rules, int percent) {
        return LevelPlay.play(survey, player, seed, pacing.stoppingAtLoss(), rules.withEnemyHealthPercent(percent))
                .result().won();
    }
}
