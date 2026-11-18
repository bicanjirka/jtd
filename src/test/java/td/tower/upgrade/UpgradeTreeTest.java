package td.tower.upgrade;

import org.junit.jupiter.api.Test;
import td.fixtures.FakeTower;
import td.fixtures.TierTrees;
import td.fixtures.WorldFixtures;
import td.tower.Tower;
import td.util.GameWorld;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static td.fixtures.TierTrees.A1;
import static td.fixtures.TierTrees.A2;
import static td.fixtures.TierTrees.A3;
import static td.fixtures.TierTrees.ALL_XP;
import static td.fixtures.TierTrees.B1;
import static td.fixtures.TierTrees.B2;
import static td.fixtures.TierTrees.LONE;
import static td.fixtures.TierTrees.MID;
import static td.fixtures.TierTrees.S1;
import static td.fixtures.TierTrees.S2;
import static td.fixtures.TierTrees.TIER_ONE;
import static td.fixtures.TierTrees.TOP;
import static td.fixtures.TierTrees.TREE;
import static td.fixtures.TierTrees.towerOwning;

class UpgradeTreeTest {

    private final GameWorld context = WorldFixtures.newWorld();

    @Test
    void aNewTowerIsOfferedTierZeroAndNothingAbove() {
        Tower tower = towerOwning(this.context, 0);

        assertThat(TREE.offered(tower, this.context)).containsExactly(A1, B1, LONE);
    }

    @Test
    void aPaidTierOffersItsUnlockOnceItsXpIsMetAndItsNodesOnlyOnceTheUnlockIsBought() {
        int xp = UpgradeTier.standard().get(1).xp();
        Tower short1 = towerOwning(this.context, xp - 1, A1);
        Tower ready = towerOwning(this.context, xp, A1);
        Tower unlocked = towerOwning(this.context, xp, A1, TIER_ONE);

        assertThat(TREE.offered(short1, this.context)).doesNotContain(TIER_ONE, A2, MID);
        assertThat(TREE.offered(ready, this.context)).contains(TIER_ONE).doesNotContain(A2, MID);
        assertThat(TREE.offered(unlocked, this.context)).contains(A2, MID).doesNotContain(TIER_ONE);
    }

    @Test
    void aFreeTierOpensByItselfOnceItsXpIsMet() {
        int xp = UpgradeTier.standard().get(2).xp();
        Tower before = towerOwning(this.context, xp - 1, A1, TIER_ONE, A2);
        Tower after = towerOwning(this.context, xp, A1, TIER_ONE, A2);

        assertThat(TREE.isOpen(2, before, this.context)).isFalse();
        assertThat(TREE.isOpen(2, after, this.context)).isTrue();
        assertThat(TREE.offered(after, this.context)).contains(A3, S1, S2);
    }

    @Test
    void aTierAboveAClosedTierStaysClosedWhateverTheXp() {
        Tower tower = towerOwning(this.context, ALL_XP, A1);

        assertThat(TREE.highestOpenTier(tower, this.context)).isZero();
        assertThat(TREE.isOpen(3, tower, this.context)).isFalse();
    }

    @Test
    void ignoredGatesWaiveTheXpButNotTheUnlock() {
        this.context.playtestRules().setUpgradeGatesIgnored(true);
        Tower tower = towerOwning(this.context, 0, A1);

        assertThat(TREE.offered(tower, this.context)).contains(TIER_ONE).doesNotContain(A2);
    }

    @Test
    void owningAClusterMemberTakesTheOtherOutOfTheOffer() {
        Tower tower = towerOwning(this.context, 0, A1);

        assertThat(TREE.offered(tower, this.context)).containsExactly(LONE);
        assertThat(TREE.isLockedOut(B1, tower.upgrades())).isTrue();
    }

    @Test
    void aNodeBehindAClusterMemberTheTowerDidNotPickIsHidden() {
        UpgradeState owned = TierTrees.state(A1);

        assertThat(TREE.isHidden(B2, owned)).isTrue();
        assertThat(TREE.isHidden(A2, owned)).isFalse();
        assertThat(TREE.branchOf(B2)).contains(B1);
    }

    @Test
    void aClusterTakesOnePlaceInItsRowAndTheLinesBehindItShareTheNext() {
        assertThat(TREE.rows().getFirst().places()).containsExactly(new RowPlace.Group(TierTrees.ROOTS, List.of(A1, B1)),
                new RowPlace.Lone(LONE));
        assertThat(TREE.rows().get(1).places()).containsExactly(new RowPlace.Branches(TierTrees.ROOTS,
                List.of(List.of(new RowPlace.Lone(A2)), List.of(new RowPlace.Lone(B2)))), new RowPlace.Lone(MID));
    }

    @Test
    void aPathToAHigherTierBuysThePaidUnlocksFirst() {
        assertThat(TREE.pathTo(TOP)).containsExactly(TIER_ONE, A1, A2, A3, TOP);
    }

    @Test
    void orReconvergesFromEitherBranch() {
        UpgradeCondition reachableFromEither = UpgradeCondition.owns(A1.id()).or(UpgradeCondition.owns(B1.id()));
        Tower viaA = FakeTower.owning(this.context, 0, 0, UpgradeState.none().with(A1));
        Tower viaB = FakeTower.owning(this.context, 1, 0, UpgradeState.none().with(B1));
        Tower viaNeither = FakeTower.owning(this.context, 2, 0, UpgradeState.none());

        assertThat(reachableFromEither.isSatisfied(viaA, this.context)).isTrue();
        assertThat(reachableFromEither.isSatisfied(viaB, this.context)).isTrue();
        assertThat(reachableFromEither.isSatisfied(viaNeither, this.context)).isFalse();
    }

    @Test
    void aTreeWithADuplicateNodeIdIsRejected() {
        UpgradeNode duplicate = UpgradeNode.of(A1.id(), "Also A", 5);

        assertThatThrownBy(() -> UpgradeTree.of(A1, duplicate)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aTreeWithANodeAboveItsTopTierIsRejected() {
        UpgradeNode high = UpgradeNode.of("high", "High", 5).inTier(UpgradeTier.standard().size());

        assertThatThrownBy(() -> UpgradeTree.of(high)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("high");
    }

    @Test
    void aNodeThatComesAfterANodeInAHigherTierIsRejected() {
        UpgradeNode early = UpgradeNode.of("early", "Early", 5).after(MID);

        assertThatThrownBy(() -> UpgradeTree.of(MID, early)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("early");
    }

    @Test
    void aClusterWhoseMembersDifferInPriceIsRejected() {
        UpgradeNode dear = UpgradeNode.of("dear", "Dear", LONE.price() + 1);

        assertThatThrownBy(() -> UpgradeTree.of(LONE, dear).withCluster(UpgradeCluster.of("Pair", LONE, dear)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aTreeWhoseNodesRequireEachOtherIsRejected() {
        UpgradeNode first = UpgradeNode.of("loop.a", "Loop A", 10).withRequires(UpgradeCondition.owns("loop.b"));
        UpgradeNode second = UpgradeNode.of("loop.b", "Loop B", 10)
                .withRequires(UpgradeCondition.owns(LONE.id()).and(UpgradeCondition.owns(first.id())));

        assertThatThrownBy(() -> UpgradeTree.of(LONE, first, second)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("loop.a");
    }

    @Test
    void aTreeWhoseNodesRequireEachOtherThroughTheDearerSideOfAnOrIsRejected() {
        UpgradeNode first = UpgradeNode.of("loop.a", "Loop A", 10).withRequires(UpgradeCondition.owns(LONE.id())
                .or(UpgradeCondition.owns(A1.id()).and(UpgradeCondition.owns("loop.b"))));
        UpgradeNode second = UpgradeNode.of("loop.b", "Loop B", 10).withRequires(UpgradeCondition.owns(first.id()));

        assertThatThrownBy(() -> UpgradeTree.of(A1, LONE, first, second)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("loop.a");
    }

    @Test
    void anOwnedNodeIsNeverOfferedAgain() {
        Tower tower = towerOwning(this.context, 0, LONE);

        assertThat(TREE.offered(tower, this.context)).doesNotContain(LONE);
    }

    @Test
    void aNodeInAClusterOfLaterTiersIsAMemberLikeAnyOther() {
        assertThat(TREE.clusterOf(S2)).contains(TierTrees.SPECIALS);
        assertThat(TREE.clusterOf(A2)).isEmpty();
    }
}
