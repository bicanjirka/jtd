package td.tower.sniper;

import td.enemy.Rank;
import td.term.TermNumbers;
import td.term.TermText;

/** Executioner: a crit that leaves an enemy below Elite under a sliver of health kills it. */
public final class ExecutionerMechanic implements SniperMechanic {

    private static final float EXECUTE_BELOW = 0.15f;
    private static final Rank SPARED_FROM = Rank.ELITE;

    @Override
    public ShotResult settle(ShotResult result, ShotActions actions) {
        if (!result.critical() || result.killed() || result.target().getRank().compareTo(SPARED_FROM) >= 0
                || result.target().getHealthFraction() >= EXECUTE_BELOW) {
            return result;
        }
        return new ShotResult(result.target(), true, actions.execute(result.target()));
    }

    @Override
    public TermText describe() {
        return TermText.of("a crit that leaves an enemy other than an Elite or a Boss under "
                + TermNumbers.percent(EXECUTE_BELOW) + " health kills it");
    }
}
