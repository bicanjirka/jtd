package td.tower;

import org.junit.jupiter.api.Test;
import td.fixtures.FakeTower;
import td.fixtures.TierTrees;
import td.fixtures.WorldFixtures;
import td.tower.proc.Proc;
import td.tower.proc.ProcPayload;
import td.tower.upgrade.UpgradeCluster;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTier;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** How a tower buys a whole path at once, and downgrades a node with everything that depends on it. */
class UpgradeOwnershipTest {

    private static final int PRICE = 10;

    private static final UpgradeNode A1 = UpgradeNode.of("a.1", "A", PRICE);
    private static final UpgradeNode B1 = UpgradeNode.of("b.1", "B", PRICE);
    private static final UpgradeNode A2 = UpgradeNode.of("a.2", "A II", PRICE).inTier(1).after(A1);
    private static final UpgradeNode HASTY = UpgradeNode.of("x.1", "Hasty", PRICE)
            .withProc(Proc.everyNthShot(1, new ProcPayload.GrantStatus(
                    TowerStatus.timed(TowerTerm.HASTE, TowerModifiers.attackSpeed(1f), 1_000))));
    private static final UpgradeTree TREE = UpgradeTree.of(A1, B1, A2, HASTY)
            .withCluster(UpgradeCluster.of("Roots", A1, B1)).withTiers(TierTrees.PAID_TIERS);

    private final GameWorld context = WorldFixtures.newWorld();

    @Test
    void buyingAPathOwnsEveryStepAndChargesTheirTotal() {
        this.context.economy().startEconomy(1_000, 5);
        FakeTower tower = this.tower();
        tower.earnXp(1_000);
        int total = TREE.planPath(A2, tower, this.context).total();

        boolean bought = tower.buyPath(A2);

        assertThat(bought).isTrue();
        assertThat(tower.upgrades().owned()).extracting(UpgradeNode::displayName)
                .containsExactlyInAnyOrder(UpgradeTier.nameOf(1), "A", "A II");
        assertThat(this.context.economy().getCredits()).isEqualTo(1_000 - total);
    }

    @Test
    void aPathTheCashDoesNotCoverBuysNothingAndSpendsNothing() {
        FakeTower tower = this.tower();
        tower.earnXp(1_000);
        int total = TREE.planPath(A2, tower, this.context).total();
        this.context.economy().startEconomy(total - 1, 5);

        boolean bought = tower.buyPath(A2);

        assertThat(bought).isFalse();
        assertThat(tower.upgrades().owned()).isEmpty();
        assertThat(this.context.economy().getCredits()).isEqualTo(total - 1);
    }

    @Test
    void aPathWithAStepShortOfXpBuysNothingAndSpendsNothing() {
        this.context.economy().startEconomy(1_000, 5);
        FakeTower tower = this.tower();

        boolean bought = tower.buyPath(A2);

        assertThat(bought).isFalse();
        assertThat(tower.upgrades().owned()).isEmpty();
        assertThat(this.context.economy().getCredits()).isEqualTo(1_000);
    }

    @Test
    void downgradingANodeRemovesEveryOwnedNodeThatDependsOnIt() {
        FakeTower tower = this.towerOwning(A2);

        boolean downgraded = tower.downgrade(A1);

        assertThat(downgraded).isTrue();
        assertThat(tower.upgrades().owned()).extracting(UpgradeNode::displayName).containsExactly(UpgradeTier.nameOf(1));
    }

    @Test
    void downgradingAPickReopensItsRival() {
        FakeTower tower = this.towerOwning(A1);

        tower.downgrade(A1);

        assertThat(tower.offeredUpgrades(this.context)).contains(A1, B1);
    }

    @Test
    void whatATowerOffersFollowsEachPurchaseAndDowngradeAfterBeingAsked() {
        FakeTower tower = this.towerOwning(TREE.unlockOf(1).orElseThrow());
        List<UpgradeNode> before = tower.offeredUpgrades(this.context);

        tower.buyUpgrade(A1);
        List<UpgradeNode> bought = tower.offeredUpgrades(this.context);
        tower.downgrade(A1);
        List<UpgradeNode> downgraded = tower.offeredUpgrades(this.context);

        assertThat(before).contains(A1, B1).doesNotContain(A2);
        assertThat(bought).contains(A2).doesNotContain(A1, B1);
        assertThat(downgraded).isEqualTo(before);
    }

    @Test
    void aDowngradeRefundsWhatSellingWouldPayLess() {
        FakeTower tower = this.towerOwning(A2);
        int sellBefore = tower.getSellPrice();
        int credits = this.context.economy().getCredits();

        tower.downgrade(A1);

        int refund = this.context.economy().getCredits() - credits;
        assertThat(refund).isPositive();
        assertThat(refund + tower.getSellPrice()).isEqualTo(sellBefore);
    }

    @Test
    void downgradingANodeTheTowerDoesNotOwnChangesNothing() {
        FakeTower tower = this.towerOwning(A1);
        int credits = this.context.economy().getCredits();

        boolean downgraded = tower.downgrade(A2);

        assertThat(downgraded).isFalse();
        assertThat(tower.upgrades().owned()).containsExactly(A1);
        assertThat(this.context.economy().getCredits()).isEqualTo(credits);
    }

    @Test
    void aStatusARemovedNodeGrantedEndsAtTheTowersNextTurn() {
        FakeTower tower = this.towerOwning(HASTY);
        tower.beginShot();
        double hasted = tower.attackRate();

        tower.downgrade(HASTY);
        tower.beginTick(1);

        assertThat(hasted).isGreaterThan(1.0);
        assertThat(tower.attackRate()).isEqualTo(1.0);
    }

    /** A tower that bought {@code targets} and their paths, gates waived. */
    private FakeTower towerOwning(UpgradeNode... targets) {
        this.context.economy().startEconomy(10_000, 5);
        this.context.playtestRules().setUpgradeGatesIgnored(true);
        FakeTower tower = this.tower();
        for (UpgradeNode target : targets) {
            assertThat(tower.buyPath(target)).as(target.displayName()).isTrue();
        }
        return tower;
    }

    private FakeTower tower() {
        FakeTower tower = FakeTower.offering(this.context, 0, 0, TREE);
        this.context.towers().add(tower);
        return tower;
    }
}
