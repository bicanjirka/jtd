package td.tower.upgrade;

import org.junit.jupiter.api.Test;
import td.fixtures.TierTrees;
import td.fixtures.WorldFixtures;
import td.tower.Tower;
import td.util.GameWorld;

import static org.assertj.core.api.Assertions.assertThat;
import static td.fixtures.TierTrees.A1;
import static td.fixtures.TierTrees.A2;
import static td.fixtures.TierTrees.ALL_XP;
import static td.fixtures.TierTrees.B1;
import static td.fixtures.TierTrees.B2;
import static td.fixtures.TierTrees.LONE;
import static td.fixtures.TierTrees.MID;
import static td.fixtures.TierTrees.S1;
import static td.fixtures.TierTrees.TIER_ONE;
import static td.fixtures.TierTrees.TREE;
import static td.fixtures.TierTrees.state;
import static td.fixtures.TierTrees.towerOwning;

/** What a tree says about a state of owned nodes: what is locked out, what a switch or a downgrade takes. */
class UpgradeTreeStateTest {

    private static final UpgradeNode EITHER = UpgradeNode.of("x.1", "X", 10)
            .withRequires(UpgradeCondition.owns(A1.id()).or(UpgradeCondition.owns(B1.id())));
    private static final UpgradeTree WITH_EITHER = UpgradeTree.of(A1, B1, EITHER).withCluster(TierTrees.ROOTS);

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
        assertThat(WITH_EITHER.isLockedOut(EITHER, state(B1))).isFalse();
    }

    @Test
    void whatAPurchaseLocksOutIsWhatItsStateLocksOutThatTheOneBeforeDidNot() {
        assertThat(TREE.newlyLockedOut(state(), state(A1))).containsExactly(B1, B2);
    }

    @Test
    void aConditionIsJudgedOnTheStateItIsGivenRatherThanTheTowersOwn() {
        Tower fresh = towerOwning(this.context, 0);

        assertThat(UpgradeCondition.owns(A1.id()).isSatisfied(fresh, state(A1), this.context)).isTrue();
        assertThat(UpgradeCondition.owns(A1.id()).isSatisfied(fresh, this.context)).isFalse();
    }

    @Test
    void aSwitchPutsTheNewMemberInTheOldOnesPlaceAndSellsWhatCameAfterTheOldOne() {
        Tower tower = towerOwning(this.context, ALL_XP, A1, TIER_ONE, A2, MID);

        UpgradeSwitch change = TREE.switchFor(B1, tower, this.context).orElseThrow();

        assertThat(change.from()).isEqualTo(A1);
        assertThat(change.sold()).containsExactly(A2);
        assertThat(change.kept().owned()).containsExactly(B1, TIER_ONE, MID);
    }

    @Test
    void thereIsNoSwitchToAMemberOfAClusterTheTowerHasNoPickIn() {
        Tower tower = towerOwning(this.context, ALL_XP, LONE);

        assertThat(TREE.switchFor(B1, tower, this.context)).isEmpty();
        assertThat(TREE.switchFor(LONE, tower, this.context)).isEmpty();
    }

    @Test
    void downgradingATiersUnlockTakesEveryNodeOfThatTierAndAbove() {
        Tower tower = towerOwning(this.context, ALL_XP, A1, TIER_ONE, A2, S1, LONE);

        UpgradeRemoval removal = TREE.removalFor(TIER_ONE, tower, this.context);

        assertThat(removal.removed()).containsExactly(TIER_ONE, A2, S1);
        assertThat(removal.kept().owned()).containsExactly(A1, LONE);
    }
}
