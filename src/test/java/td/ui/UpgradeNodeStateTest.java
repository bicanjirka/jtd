package td.ui;

import org.junit.jupiter.api.Test;
import td.fixtures.FakeTower;
import td.fixtures.WorldFixtures;
import td.tower.upgrade.ExclusiveChoice;
import td.tower.upgrade.StandardBaseSlot;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTier;
import td.tower.upgrade.UpgradeTree;
import td.ui.UpgradeNodeState.Look;
import td.util.GameWorld;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class UpgradeNodeStateTest {

    private static final int LIST_PRICE = 10;
    private static final UpgradeNode A1 = UpgradeTier.HEAD_1.node("a.1", "A", LIST_PRICE);
    private static final UpgradeNode A2 = UpgradeTier.HEAD_2.node("a.2", "A II", LIST_PRICE).after(A1);
    private static final UpgradeNode A3 = UpgradeTier.HEAD_3.node("a.3", "A III", LIST_PRICE).after(A2);
    private static final UpgradeNode B1 = UpgradeTier.HEAD_1.node("b.1", "B", LIST_PRICE);
    private static final UpgradeTree TREE = UpgradeTree.of(StandardBaseSlot.nodes(LIST_PRICE, 4f, A3))
            .with(A1, A2, A3, B1)
            .withChoice(ExclusiveChoice.oneOf(A1, B1));

    private final GameWorld world = WorldFixtures.newWorld();

    @Test
    void aNodeACoreStepHoldsBackIsSealedByThatStep() {
        FakeTower tower = this.tower();

        UpgradeNodeState state = UpgradeNodeState.of(A1, tower, this.world);

        assertThat(state.look()).isEqualTo(Look.SEALED);
        assertThat(state.sealedBy()).contains(core(StandardBaseSlot.ATTUNE_ID));
    }

    @Test
    void aNodeWhoseLineIsNotYetBoughtNeedsThePreviousOne() {
        FakeTower tower = this.tower();
        tower.buyPath(core(StandardBaseSlot.ATTUNE_ID));

        assertThat(UpgradeNodeState.of(A2, tower, this.world).look()).isEqualTo(Look.NEEDS_PREVIOUS);
    }

    @Test
    void aNodeShortOfXpWaitsWithARingOfItsShare() {
        FakeTower tower = this.tower();
        tower.buyPath(A1);
        tower.earnXp(A2.xp() / 2);

        UpgradeNodeState state = UpgradeNodeState.of(A2, tower, this.world);

        assertThat(state.look()).isEqualTo(Look.WAITING);
        assertThat(state.ring()).isCloseTo(0.5f, within(0.01f));
    }

    @Test
    void anOpenNodeIsBuyableOrShortOfCashByWhatTheCreditsCover() {
        FakeTower tower = this.tower();
        tower.buyPath(core(StandardBaseSlot.ATTUNE_ID));

        Look covered = UpgradeNodeState.of(A1, tower, this.world).look();
        this.world.economy().startEconomy(A1.price() - 1, 5);
        Look uncovered = UpgradeNodeState.of(A1, tower, this.world).look();

        assertThat(covered).isEqualTo(Look.BUYABLE);
        assertThat(uncovered).isEqualTo(Look.SHORT_OF_CASH);
    }

    @Test
    void aPickLocksOutItsRivalAndIsOwned() {
        FakeTower tower = this.tower();
        tower.buyPath(A1);

        assertThat(UpgradeNodeState.of(A1, tower, this.world).look()).isEqualTo(Look.OWNED);
        assertThat(UpgradeNodeState.of(B1, tower, this.world).look()).isEqualTo(Look.LOCKED_OUT);
    }

    private FakeTower tower() {
        this.world.economy().startEconomy(1_000, 5);
        return FakeTower.offering(this.world, 0, 0, TREE);
    }

    private static UpgradeNode core(String id) {
        return TREE.node(id).orElseThrow();
    }
}
