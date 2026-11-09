package td.enemy;

import td.economy.EconomyDelta;
import td.economy.EconomyLedger;
import td.util.PlaytestRules;

/**
 * Settles a finished walk with the economy: a kill pays its bounty and a score its rank weighs, a leak costs that
 * bounty in score and the lives it takes.
 */
public final class WalkSettlement implements WalkEndListener {

    private final EconomyLedger economy;
    private final PlaytestRules rules;

    public WalkSettlement(EconomyLedger economy, PlaytestRules rules) {
        this.economy = economy;
        this.rules = rules;
    }

    @Override
    public void walkEnded(EnemyWalk walk) {
        this.economy.apply(switch (walk.fate()) {
            case KILLED -> EconomyDelta.kill(walk.getBounty(), Math.round(walk.getBounty() * walk.getRank().scoreMultiplier()));
            case LEAKED -> EconomyDelta.leak(walk.getBounty(), this.livesTaken(walk));
            case ALIVE, HATCHED -> throw new IllegalArgumentException("A walk ends killed or leaked, not " + walk.fate());
        });
    }

    /** Held to the lives left, so an enemy worth every life takes exactly those. */
    private int livesTaken(EnemyWalk walk) {
        if (this.rules.livesUnlimited()) {
            return 0;
        }
        return Math.min(walk.livesOnLeak(), Math.max(1, this.economy.getLives()));
    }
}
