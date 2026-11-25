package td.tower.sniper;

import td.term.TermNumbers;
import td.term.TermText;
import td.tower.targeting.ExecuteThreshold;

/** Executioner: a crit that leaves an enemy below Elite under a sliver of health kills it. */
public final class ExecutionerMechanic implements SniperMechanic {

    private static final ExecuteThreshold LINE = ExecuteThreshold.EXECUTIONER;

    @Override
    public ShotResult settle(ShotResult result, ShotActions actions) {
        if (!result.critical() || result.killed() || !LINE.executes(result.target())) {
            return result;
        }
        return new ShotResult(result.target(), true, actions.execute(result.target()));
    }

    @Override
    public TermText describe() {
        return TermText.of("a crit that leaves an enemy other than an Elite or a Boss under "
                + TermNumbers.percent(LINE.below()) + " health kills it");
    }
}
