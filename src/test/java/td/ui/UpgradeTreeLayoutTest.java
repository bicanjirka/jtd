package td.ui;

import org.junit.jupiter.api.Test;
import td.fixtures.WorldFixtures;
import td.tower.TowerFactory;
import td.tower.upgrade.ExclusiveChoice;
import td.tower.upgrade.StandardBaseSlot;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTier;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UpgradeTreeLayoutTest {

    private static final int LIST_PRICE = 10;

    private static final UpgradeNode A1 = UpgradeTier.HEAD_1.node("a.1", "A", LIST_PRICE);
    private static final UpgradeNode A2 = UpgradeTier.HEAD_2.node("a.2", "A II", LIST_PRICE).after(A1);
    private static final UpgradeNode A3 = UpgradeTier.HEAD_3.node("a.3", "A III", LIST_PRICE).after(A2);
    private static final UpgradeNode A4A = UpgradeTier.HEAD_4.node("a.4a", "A IV-A", LIST_PRICE).after(A3);
    private static final UpgradeNode A4B = UpgradeTier.HEAD_4.node("a.4b", "A IV-B", LIST_PRICE).after(A3);
    private static final UpgradeNode B1 = UpgradeTier.HEAD_1.node("b.1", "B", LIST_PRICE);
    private static final UpgradeNode X1 = UpgradeTier.EXTRA_1.node("x.1", "X", LIST_PRICE);

    private final GameWorld context = WorldFixtures.newWorld();

    @Test
    void everyBuiltInTreeFitsThePanelWithEveryNodePlaced() {
        for (TowerFactory.Type type : TowerFactory.Type.values()) {
            UpgradeTree tree = TowerFactory.createTower(type, this.context, 0, 0).upgradeTree();

            UpgradeTreeLayout layout = UpgradeTreeLayout.of(tree);

            assertThat(layout.spots()).as(type.name()).hasSize(tree.nodes().size());
        }
    }

    @Test
    void aLineRunsLeftToRightByLevelAndAPairOfLevelFoursForksAboveAndBelowIt() {
        UpgradeTreeLayout layout = UpgradeTreeLayout.of(tree().with(A4A, A4B).withChoice(ExclusiveChoice.oneOf(A4A, A4B)));

        UpgradeTreeLayout.Spot one = layout.spotOf(A1).orElseThrow();
        UpgradeTreeLayout.Spot three = layout.spotOf(A3).orElseThrow();

        assertThat(three.x()).isGreaterThan(one.x());
        assertThat(three.y()).isEqualTo(one.y());
        assertThat(layout.spotOf(A4A).orElseThrow().y()).isLessThan(one.y());
        assertThat(layout.spotOf(A4B).orElseThrow().y()).isGreaterThan(one.y());
        assertThat(layout.forks()).singleElement().satisfies(fork -> assertThat(fork.members()).containsExactly(A4A, A4B));
    }

    @Test
    void theExclusiveHeadRootsMeetAtADiamondBetweenTheirRows() {
        UpgradeTreeLayout layout = UpgradeTreeLayout.of(tree());

        float a = layout.spotOf(A1).orElseThrow().y();
        float b = layout.spotOf(B1).orElseThrow().y();

        assertThat(layout.rootChoice()).hasValueSatisfying(choice -> assertThat(choice.y()).isEqualTo((a + b) / 2));
    }

    @Test
    void twoLinesOnOneRowAreRejected() {
        UpgradeNode another = UpgradeTier.EXTRA_1.node("y.1", "Y", LIST_PRICE);

        assertThatThrownBy(() -> UpgradeTreeLayout.of(UpgradeTree.of(X1, another)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("share a place");
    }

    @Test
    void moreSpecialsThanTheHaloHoldsAreRejected() {
        List<UpgradeNode> opening = List.of(A1, A1, A1, B1, B1, B1, X1);
        UpgradeTree tree = UpgradeTree.of(A1, B1, X1);
        for (int i = 0; i < opening.size(); i++) {
            tree = tree.with(UpgradeTier.SPECIAL.node("s." + i, "S" + i, LIST_PRICE).after(opening.get(i)));
        }
        UpgradeTree sevenSpecials = tree;

        assertThatThrownBy(() -> UpgradeTreeLayout.of(sevenSpecials))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("specials");
    }

    private static UpgradeTree tree() {
        return UpgradeTree.of(StandardBaseSlot.nodes(LIST_PRICE, 4f, A3))
                .with(A1, A2, A3, B1, X1)
                .withChoice(ExclusiveChoice.oneOf(A1, B1));
    }
}
