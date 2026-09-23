package td.tower.upgrade;

import org.junit.jupiter.api.Test;
import td.fixtures.WorldFixtures;
import td.tower.Tower;
import td.util.GameWorld;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UpgradeTreeTest {

    private final GameWorld context = WorldFixtures.newWorld();

    private static final UpgradeNode RANGE = StandardBaseSlot.rangeNode(10);
    private static final UpgradeNode AWAKEN = StandardBaseSlot.awakenNode(20);
    private static final UpgradeNode HEAD_ROOT_A = UpgradeNode.of("head.a.1", UpgradeSlot.HEAD, "Root A", 10)
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.HEAD));
    private static final UpgradeNode HEAD_A_LV2 = UpgradeNode.of("head.a.2", UpgradeSlot.HEAD, "A lv2", 15)
            .withRequires(UpgradeCondition.owns(HEAD_ROOT_A.id()));
    private static final UpgradeNode HEAD_ROOT_B = UpgradeNode.of("head.b.1", UpgradeSlot.HEAD, "Root B", 10)
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.HEAD));
    private static final UpgradeNode SPECIAL_ROOT = UpgradeNode.of("special.1", UpgradeSlot.SPECIAL, "Special", 10)
            .withRequires(StandardBaseSlot.opens(UpgradeSlot.SPECIAL));

    private static final UpgradeTree TREE = UpgradeTree.of(RANGE, AWAKEN, HEAD_ROOT_A, HEAD_A_LV2, HEAD_ROOT_B,
            SPECIAL_ROOT);

    @Test
    void offeredListsOnlyBaseNodesBeforeAwakenIsBought() {
        Tower tower = new FakeUpgradeTower(context, 0, 0, UpgradeState.none());

        assertThat(TREE.offered(tower, context)).containsExactly(RANGE, AWAKEN);
    }

    @Test
    void offeredOrdersBySlotThenByDeclarationOrderWithinASlot() {
        UpgradeState afterAwaken = UpgradeState.none().with(AWAKEN);
        Tower tower = new FakeUpgradeTower(context, 0, 0, afterAwaken);

        assertThat(TREE.offered(tower, context)).containsExactly(RANGE, HEAD_ROOT_A, HEAD_ROOT_B, SPECIAL_ROOT);
    }

    @Test
    void choosingOneHeadRootForeclosesTheOtherRootForever() {
        UpgradeState afterRootA = UpgradeState.none().with(AWAKEN).with(HEAD_ROOT_A);
        Tower tower = new FakeUpgradeTower(context, 0, 0, afterRootA);

        List<UpgradeNode> offered = TREE.offered(tower, context);

        assertThat(offered).contains(HEAD_A_LV2).doesNotContain(HEAD_ROOT_B);
    }

    @Test
    void aChainsNextLevelIsOfferedOnlyOnceItsOwnPredecessorIsOwned() {
        Tower beforeRootA = new FakeUpgradeTower(context, 0, 0, UpgradeState.none().with(AWAKEN));
        Tower afterRootA = new FakeUpgradeTower(context, 1, 0, UpgradeState.none().with(AWAKEN).with(HEAD_ROOT_A));

        assertThat(TREE.offered(beforeRootA, context)).doesNotContain(HEAD_A_LV2);
        assertThat(TREE.offered(afterRootA, context)).contains(HEAD_A_LV2);
    }

    @Test
    void anAlreadyOwnedNodeIsNeverOfferedAgain() {
        UpgradeState afterRange = UpgradeState.none().with(RANGE);
        Tower tower = new FakeUpgradeTower(context, 0, 0, afterRange);

        assertThat(TREE.offered(tower, context)).doesNotContain(RANGE);
    }

    @Test
    void orReconvergesFromEitherBranch() {
        UpgradeCondition reachableFromEither = UpgradeCondition.owns(HEAD_ROOT_A.id())
                .or(UpgradeCondition.owns(HEAD_ROOT_B.id()));
        Tower viaA = new FakeUpgradeTower(context, 0, 0, UpgradeState.none().with(HEAD_ROOT_A));
        Tower viaB = new FakeUpgradeTower(context, 1, 0, UpgradeState.none().with(HEAD_ROOT_B));
        Tower viaNeither = new FakeUpgradeTower(context, 2, 0, UpgradeState.none());

        assertThat(reachableFromEither.isSatisfied(viaA, context)).isTrue();
        assertThat(reachableFromEither.isSatisfied(viaB, context)).isTrue();
        assertThat(reachableFromEither.isSatisfied(viaNeither, context)).isFalse();
    }

    @Test
    void aTreeWithADuplicateNodeIdIsRejected() {
        UpgradeNode duplicate = UpgradeNode.of(RANGE.id(), UpgradeSlot.BASE, "Also range", 5);

        assertThatThrownBy(() -> UpgradeTree.of(RANGE, duplicate))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void progressReadsLiveProgressTowardAKillCountGate() {
        UpgradeNode gated = UpgradeNode.of("gated", UpgradeSlot.HEAD, "Gated", 10)
                .withGate(new KillCountCondition(10));
        FakeUpgradeTower tower = new FakeUpgradeTower(context, 0, 0, UpgradeState.none());
        tower.setKillCount(4);

        assertThat(gated.gate().progress(tower, context)).isEqualTo("4/10 kills");
    }
}
