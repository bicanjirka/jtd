package td.tower;

import org.junit.jupiter.api.Test;
import td.fixtures.FakeTower;
import td.fixtures.WorldFixtures;
import td.tower.upgrade.ExclusiveChoice;
import td.tower.upgrade.StandardBaseSlot;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTier;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** How a tower buys a whole path at once. */
class UpgradeOwnershipTest {

    private static final int LIST_PRICE = 10;

    private static final UpgradeNode A1 = UpgradeTier.HEAD_1.node("a.1", "A", LIST_PRICE);
    private static final UpgradeNode A2 = UpgradeTier.HEAD_2.node("a.2", "A II", LIST_PRICE).after(A1);
    private static final UpgradeNode A3 = UpgradeTier.HEAD_3.node("a.3", "A III", LIST_PRICE).after(A2);
    private static final UpgradeNode B1 = UpgradeTier.HEAD_1.node("b.1", "B", LIST_PRICE);
    private static final List<UpgradeNode> SHARED = StandardBaseSlot.nodes(LIST_PRICE, 4f, A3);
    private static final UpgradeTree TREE = UpgradeTree.of(SHARED).with(A1, A2, A3, B1)
            .withChoice(ExclusiveChoice.oneOf(A1, B1));

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

    private FakeTower tower() {
        FakeTower tower = FakeTower.offering(this.context, 0, 0, TREE);
        this.context.towers().add(tower);
        return tower;
    }
}
