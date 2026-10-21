package td.tower.sniper;

import td.enemy.Rank;
import td.term.TermNumbers;
import td.term.TermText;

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

    @Override
    public TermText describe() {
        return TermText.of("a shot that leaves an enemy other than a boss under " + TermNumbers.percent(EXECUTE_BELOW)
                + " health kills it; an execution counts as a crit");
    }
}
