package td.tower;

import org.junit.jupiter.api.Test;
import td.fixtures.FakeTower;
import td.fixtures.WorldFixtures;
import td.tower.proc.Proc;
import td.tower.proc.ProcPayload;
import td.tower.upgrade.ExclusiveChoice;
import td.tower.upgrade.StandardBaseSlot;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTier;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** How a tower buys a whole path at once, and downgrades a node with everything that depends on it. */
class UpgradeOwnershipTest {

    private static final int LIST_PRICE = 10;

    private static final UpgradeNode A1 = UpgradeTier.HEAD_1.node("a.1", "A", LIST_PRICE);
    private static final UpgradeNode A2 = UpgradeTier.HEAD_2.node("a.2", "A II", LIST_PRICE).after(A1);
    private static final UpgradeNode A3 = UpgradeTier.HEAD_3.node("a.3", "A III", LIST_PRICE).after(A2);
    private static final UpgradeNode B1 = UpgradeTier.HEAD_1.node("b.1", "B", LIST_PRICE);
    private static final UpgradeNode S1 = UpgradeTier.SPECIAL.node("s.1", "S1", LIST_PRICE);
    private static final UpgradeNode S2 = UpgradeTier.SPECIAL.node("s.2", "S2", LIST_PRICE);
    private static final UpgradeNode S3 = UpgradeTier.SPECIAL.node("s.3", "S3", LIST_PRICE);
    private static final UpgradeNode HASTY = UpgradeTier.EXTRA_1.node("x.1", "Hasty", LIST_PRICE)
            .withProc(Proc.everyNthShot(1, new ProcPayload.GrantStatus(
                    TowerStatus.timed(TowerTerm.HASTE, TowerModifiers.attackSpeed(1f), 1_000))));
    private static final List<UpgradeNode> SHARED = StandardBaseSlot.nodes(LIST_PRICE, 4f, A3);
    private static final UpgradeTree TREE = UpgradeTree.of(SHARED).with(A1, A2, A3, B1, S1, S2, S3, HASTY)
            .withChoice(ExclusiveChoice.oneOf(A1, B1))
            .withChoice(ExclusiveChoice.specials(S1, S2, S3));

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
                .containsExactlyInAnyOrder("Attune", "A", "A II");
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
        assertThat(tower.upgrades().owned()).extracting(UpgradeNode::displayName).containsExactly("Attune");
    }

    @Test
    void downgradingTranscendentRemovesTheLaterOfTwoSpecials() {
        FakeTower tower = this.towerOwning(S1, A3, core(StandardBaseSlot.TRANSCENDENT_ID), S2);

        tower.downgrade(core(StandardBaseSlot.TRANSCENDENT_ID));

        assertThat(tower.upgrades().owns(S1.id())).isTrue();
        assertThat(tower.upgrades().owns(S2.id())).isFalse();
        assertThat(tower.upgrades().owns(A3.id())).isTrue();
    }

    @Test
    void downgradingAPickReopensItsRival() {
        FakeTower tower = this.towerOwning(A1);

        tower.downgrade(A1);

        assertThat(tower.offeredUpgrades(this.context)).contains(A1, B1);
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
        assertThat(tower.upgrades().owned()).hasSize(2);
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

    private static UpgradeNode core(String id) {
        return SHARED.stream().filter(n -> n.id().equals(id)).findFirst().orElseThrow();
    }

    private FakeTower tower() {
        FakeTower tower = FakeTower.offering(this.context, 0, 0, TREE);
        this.context.towers().add(tower);
        return tower;
    }
}
