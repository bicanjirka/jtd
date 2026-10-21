package td.tower.sniper;

import td.enemy.Rank;
import td.term.TermText;
import td.tower.upgrade.PerkText;

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
        return TermText.of("a non-boss enemy left under " + PerkText.percent(EXECUTE_BELOW) + " health by a shot dies; executions count as crits");
    }
}
