package td.tower.upgrade;

import org.junit.jupiter.api.Test;
import td.fixtures.FakeTower;
import td.fixtures.WorldFixtures;
import td.tower.Tower;
import td.util.GameWorld;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** What a tree says about a state of owned nodes: what is locked out for good, and which Core step seals a node. */
class UpgradeTreeStateTest {

    private static final int LIST_PRICE = 10;

    private static final UpgradeNode A1 = UpgradeTier.HEAD_1.node("a.1", "A", LIST_PRICE);
    private static final UpgradeNode A2 = UpgradeTier.HEAD_2.node("a.2", "A II", LIST_PRICE).after(A1);
    private static final UpgradeNode A3 = UpgradeTier.HEAD_3.node("a.3", "A III", LIST_PRICE).after(A2);
    private static final UpgradeNode B1 = UpgradeTier.HEAD_1.node("b.1", "B", LIST_PRICE);
    private static final UpgradeNode B2 = UpgradeTier.HEAD_2.node("b.2", "B II", LIST_PRICE).after(B1);
    private static final UpgradeNode EITHER = UpgradeTier.EXTRA_1.node("x.1", "X", LIST_PRICE)
            .withRequires(UpgradeCondition.owns(A1.id()).or(UpgradeCondition.owns(B1.id())));
    private static final UpgradeNode S1 = UpgradeTier.SPECIAL.node("s.1", "S1", LIST_PRICE);
    private static final UpgradeNode S2 = UpgradeTier.SPECIAL.node("s.2", "S2", LIST_PRICE);
    private static final UpgradeNode S3 = UpgradeTier.SPECIAL.node("s.3", "S3", LIST_PRICE);
    private static final List<UpgradeNode> SHARED = StandardBaseSlot.nodes(LIST_PRICE, 4f, A3);

    private static final UpgradeTree TREE = UpgradeTree.of(SHARED)
            .with(A1, A2, A3, B1, B2, EITHER, S1, S2, S3)
            .withChoice(ExclusiveChoice.oneOf(A1, B1))
            .withChoice(ExclusiveChoice.specials(S1, S2, S3));

    private final GameWorld context = WorldFixtures.newWorld();

    @Test
    void aPickLocksOutItsRivalAndEveryNodeBehindIt() {
        UpgradeState owned = state(A1);

        assertThat(TREE.isLockedOut(B1, owned)).isTrue();
        assertThat(TREE.isLockedOut(B2, owned)).isTrue();
        assertThat(TREE.isLockedOut(A2, owned)).isFalse();
    }

    @Test
    void aNodeEitherSideOfAnOrOpensStaysOpenWhenOneSideIsLockedOut() {
        assertThat(TREE.isLockedOut(EITHER, state(B1))).isFalse();
    }

    @Test
    void everyUnownedSpecialIsLockedOutOnceThePicksAreUsed() {
        UpgradeState owned = state(S1, S2);

        assertThat(TREE.isLockedOut(S3, owned)).isTrue();
        assertThat(TREE.isLockedOut(S3, state(S1))).isFalse();
    }

    @Test
    void whatAPurchaseLocksOutIsWhatItsStateLocksOutThatTheOneBeforeDidNot() {
        assertThat(TREE.newlyLockedOut(state(), state(A1))).containsExactly(B1, B2);
    }

    @Test
    void aNodeIsSealedByTheFirstCoreStepThatOpensIt() {
        Tower fresh = this.towerOwning();
        Tower attuned = this.towerOwning(core(StandardBaseSlot.ATTUNE_ID));
        Tower awakened = this.towerOwning(core(StandardBaseSlot.ATTUNE_ID), core(StandardBaseSlot.AWAKEN_ID));

        assertThat(TREE.sealedBy(A2, fresh, this.context)).contains(core(StandardBaseSlot.ATTUNE_ID));
        assertThat(TREE.sealedBy(A3, attuned, this.context)).contains(core(StandardBaseSlot.AWAKEN_ID));
        assertThat(TREE.sealedBy(A3, awakened, this.context)).isEmpty();
    }

    @Test
    void aSecondSpecialIsSealedByTranscendent() {
        Tower tower = this.towerOwning(core(StandardBaseSlot.ATTUNE_ID), core(StandardBaseSlot.AWAKEN_ID), S1);

        assertThat(TREE.sealedBy(S2, tower, this.context)).contains(core(StandardBaseSlot.TRANSCENDENT_ID));
    }

    @Test
    void aConditionIsJudgedOnTheStateItIsGivenRatherThanTheTowersOwn() {
        Tower fresh = this.towerOwning();

        assertThat(UpgradeCondition.owns(A1.id()).isSatisfied(fresh, state(A1), this.context)).isTrue();
        assertThat(UpgradeCondition.owns(A1.id()).isSatisfied(fresh, this.context)).isFalse();
    }

    @Test
    void anOrNeedsNothingOnceASideIsMetAndOtherwiseTheSideAPurchaseCanMeet() {
        UpgradeCondition secondSpecial = UpgradeCondition.slotEmpty(UpgradeSlot.SPECIAL)
                .or(UpgradeCondition.owns(StandardBaseSlot.TRANSCENDENT_ID));
        Tower fresh = this.towerOwning();

        assertThat(secondSpecial.missingNodes(fresh, state(), this.context)).isEmpty();
        assertThat(secondSpecial.missingNodes(fresh, state(S1), this.context))
                .containsExactly(StandardBaseSlot.TRANSCENDENT_ID);
    }

    private Tower towerOwning(UpgradeNode... owned) {
        return FakeTower.owning(this.context, 0, 0, state(owned));
    }

    private static UpgradeState state(UpgradeNode... owned) {
        return new UpgradeState(List.of(owned));
    }

    private static UpgradeNode core(String id) {
        return SHARED.stream().filter(n -> n.id().equals(id)).findFirst().orElseThrow();
    }
}
