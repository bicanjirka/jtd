package td.tower.sniper;

import td.enemy.Rank;

/**
 * Executioner: a shot that leaves a non-boss enemy under a sliver of health kills it, and counts
 * as a crit for what triggers off one.
 */
public final class ExecutionerPerk implements SniperPerk {

    private static final float EXECUTE_BELOW = 0.15f;

    @Override
    public ShotResult settle(ShotResult result, ShotActions actions) {
        if (result.killed() || result.target().getRank() == Rank.BOSS
                || result.target().getHealthFraction() >= EXECUTE_BELOW) {
            return result;
        }
        return new ShotResult(result.target(), true, actions.execute(result.target()));
    }
}
