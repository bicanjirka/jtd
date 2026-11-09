package td.enemy;

import org.junit.jupiter.api.Test;
import td.economy.EconomyLedger;
import td.economy.EconomyState;
import td.util.PlaytestRules;

import static org.assertj.core.api.Assertions.assertThat;

class WalkSettlementTest {

    private final EconomyLedger economy = new EconomyLedger();
    private final WalkSettlement settlement = new WalkSettlement(this.economy, new PlaytestRules());

    private record Walk(int getBounty, Rank getRank, EnemyInspection.Fate fate, int livesOnLeak) implements EnemyWalk {

        @Override
        public long entryOrdinal() {
            return 0;
        }

        @Override
        public boolean walkedWithin(double x, double y, double radius) {
            return false;
        }
    }

    @Test
    void aKillPaysItsBountyAndScoresItWeighedByItsRank() {
        this.economy.startEconomy(0, 10);

        this.settlement.walkEnded(new Walk(20, Rank.BOSS, EnemyInspection.Fate.KILLED, 1));

        assertThat(this.economy.state()).isEqualTo(new EconomyState(20, Math.round(20 * Rank.BOSS.scoreMultiplier()), 10));
    }

    @Test
    void aLeakTakesNoMoreLivesThanAreLeft() {
        this.economy.startEconomy(0, 3);

        this.settlement.walkEnded(new Walk(20, Rank.GRUNT, EnemyInspection.Fate.LEAKED, 5));

        assertThat(this.economy.state()).isEqualTo(new EconomyState(0, -20, 0));
    }
}
