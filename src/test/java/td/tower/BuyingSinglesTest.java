package td.tower;

import org.junit.jupiter.api.Test;
import td.fixtures.FakeTower;
import td.fixtures.WorldFixtures;
import td.tower.upgrade.UpgradeCluster;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** A tier's singles, the nodes in no cluster, bought in one go. */
class BuyingSinglesTest {

    private static final UpgradeNode LEFT = UpgradeNode.of("left", "Left", 10);
    private static final UpgradeNode RIGHT = UpgradeNode.of("right", "Right", 10);
    private static final UpgradeNode CHEAP = UpgradeNode.of("cheap", "Cheap", 10);
    private static final UpgradeNode DEAR = UpgradeNode.of("dear", "Dear", 30);
    private static final UpgradeNode LAST = UpgradeNode.of("last", "Last", 5);
    private static final UpgradeNode AFTER_LEFT = UpgradeNode.of("after.left", "After Left", 5).after(LEFT);
    private static final UpgradeNode HIGHER = UpgradeNode.of("higher", "Higher", 5).inTier(1);
    private static final UpgradeTree TREE = UpgradeTree.of(LEFT, RIGHT, CHEAP, DEAR, LAST, AFTER_LEFT, HIGHER)
            .withCluster(UpgradeCluster.of("Side", LEFT, RIGHT));

    private final GameWorld context = WorldFixtures.newWorld();

    @Test
    void aTiersSinglesAreItsNodesInNoCluster() {
        assertThat(TREE.singlesIn(0)).containsExactly(CHEAP, DEAR, LAST, AFTER_LEFT);
        assertThat(TREE.singlesIn(1)).containsExactly(HIGHER);
    }

    @Test
    void buyingEachGoesInOrderAndPassesOverWhatTheCreditsCannotPayFor() {
        FakeTower tower = this.towerWith(20);

        int bought = tower.buyEach(List.of(CHEAP, DEAR, LAST));

        assertThat(bought).isEqualTo(2);
        assertThat(tower.upgrades().owned()).containsExactly(CHEAP, LAST);
        assertThat(this.context.economy().getCredits()).isEqualTo(20 - CHEAP.price() - LAST.price());
    }

    @Test
    void aSingleWhoseNodeBeforeIsNotOwnedIsPassedOver() {
        FakeTower tower = this.towerWith(1_000);

        tower.buyEach(TREE.singlesIn(0));

        assertThat(tower.upgrades().owned()).containsExactly(CHEAP, DEAR, LAST);
    }

    @Test
    void aLockedTiersSinglesBuyNothing() {
        FakeTower tower = this.towerWith(1_000);

        int bought = tower.buyEach(TREE.singlesIn(1));

        assertThat(bought).isZero();
        assertThat(this.context.economy().getCredits()).isEqualTo(1_000);
    }

    @Test
    void aTierWhoseSinglesAreAllOwnedBuysNothingMore() {
        FakeTower tower = this.towerWith(1_000);
        tower.buyEach(TREE.singlesIn(0));
        int credits = this.context.economy().getCredits();

        int bought = tower.buyEach(TREE.singlesIn(0));

        assertThat(bought).isZero();
        assertThat(this.context.economy().getCredits()).isEqualTo(credits);
    }

    private FakeTower towerWith(int credits) {
        this.context.economy().startEconomy(credits, 5);
        return FakeTower.offering(this.context, 0, 0, TREE);
    }
}
