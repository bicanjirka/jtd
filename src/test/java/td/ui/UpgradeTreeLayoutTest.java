package td.ui;

import org.junit.jupiter.api.Test;
import td.fixtures.WorldFixtures;
import td.tower.TowerFactory;
import td.tower.upgrade.UpgradeCluster;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeState;
import td.tower.upgrade.UpgradeTree;
import td.ui.UpgradeTreeLayout.Spot;
import td.util.GameWorld;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static td.fixtures.TierTrees.A1;
import static td.fixtures.TierTrees.A2;
import static td.fixtures.TierTrees.A3;
import static td.fixtures.TierTrees.B1;
import static td.fixtures.TierTrees.B2;
import static td.fixtures.TierTrees.LONE;
import static td.fixtures.TierTrees.MID;
import static td.fixtures.TierTrees.TIER_ONE;
import static td.fixtures.TierTrees.TOP;
import static td.fixtures.TierTrees.TREE;
import static td.fixtures.TierTrees.state;

class UpgradeTreeLayoutTest {

    private final GameWorld context = WorldFixtures.newWorld();

    @Test
    void everyBuiltInTreeFitsThePanelWithEveryNodeItShowsPlaced() {
        for (TowerFactory.Type type : TowerFactory.Type.values()) {
            UpgradeTree tree = TowerFactory.createTower(type, this.context, 0, 0).upgradeTree();

            UpgradeTreeLayout layout = layout(tree, UpgradeState.none());

            assertThat(tree.nodes()).as(type.name()).filteredOn(node -> !tree.isHidden(node, UpgradeState.none()))
                    .allMatch(node -> layout.spotOf(node).isPresent());
            assertThat(layout.spots().values()).as(type.name()).allMatch(spot -> spot.x() - spot.width() / 2 >= 0f
                    && spot.x() + spot.width() / 2 <= UpgradeTreeLayout.WIDTH);
            assertThat(layout.height()).as(type.name()).isLessThanOrEqualTo(UpgradeTreeLayout.HEIGHT);
        }
    }

    @Test
    void tiersClimbFromTheBottomWithAPaidTiersUnlockInTheBandBetween() {
        UpgradeTreeLayout layout = layout(TREE, state(A1));

        float zero = layout.spotOf(A1).orElseThrow().y();
        float one = layout.spotOf(A2).orElseThrow().y();
        float unlock = layout.spotOf(TIER_ONE).orElseThrow().y();

        assertThat(one).isLessThan(zero);
        assertThat(unlock).isBetween(one, zero);
    }

    @Test
    void aRowRunsLeftToRightInTheTreesOrderWithAClustersMembersInOneFrame() {
        UpgradeTreeLayout layout = layout(TREE, UpgradeState.none());

        assertThat(layout.spotOf(A1).orElseThrow().x()).isLessThan(layout.spotOf(B1).orElseThrow().x());
        assertThat(layout.spotOf(B1).orElseThrow().x()).isLessThan(layout.spotOf(LONE).orElseThrow().x());
        assertThat(layout.frames()).filteredOn(frame -> frame.cluster().name().equals("Roots")).singleElement()
                .satisfies(frame -> assertThat(frame.left()).isLessThan(layout.spotOf(A1).orElseThrow().x()));
    }

    @Test
    void theBranchOfTheOwnedMemberShowsAndTheOthersHidesBehindAMarkerInEachRowItReaches() {
        UpgradeTreeLayout layout = layout(TREE, state(B1));

        assertThat(layout.spotOf(B2)).isPresent();
        assertThat(layout.spotOf(A2)).isEmpty();
        assertThat(layout.spotOf(TOP)).isEmpty();
        assertThat(layout.hidden()).hasSize(3);
        assertThat(layout.links()).contains(new UpgradeTreeLayout.Link(B1, B2));
    }

    @Test
    void beforeAPickNoBranchShowsAndHoveringAMemberPreviewsItsOwnDimmed() {
        UpgradeTreeLayout none = layout(TREE, UpgradeState.none());
        UpgradeTreeLayout previewed = UpgradeTreeLayout.of(TREE, UpgradeState.none(), Optional.of(A1), Set.of());

        assertThat(none.spotOf(A2)).isEmpty();
        assertThat(none.spotOf(B2)).isEmpty();
        assertThat(previewed.spotOf(A2)).isPresent();
        assertThat(previewed.previewed()).containsExactlyInAnyOrder(A2, A3, TOP);
    }

    @Test
    void aFoldedTierIsAThinRowOfSmallNodes() {
        UpgradeTreeLayout open = layout(TREE, state(A1, LONE));
        UpgradeTreeLayout folded = UpgradeTreeLayout.of(TREE, state(A1, LONE), Optional.empty(), Set.of(0));

        Spot full = open.spotOf(LONE).orElseThrow();
        Spot small = folded.spotOf(LONE).orElseThrow();

        assertThat(small.width()).isLessThan(full.width());
        assertThat(folded.height()).isLessThan(open.height());
        assertThat(folded.spotOf(MID).orElseThrow().y()).isEqualTo(open.spotOf(MID).orElseThrow().y());
    }

    @Test
    void theForksSidesStandInTheGateBandUnderTheirTier() {
        UpgradeNode left = UpgradeNode.of("side.l", "Left", 10).inTier(1);
        UpgradeNode right = UpgradeNode.of("side.r", "Right", 10).inTier(1);
        UpgradeNode follow = UpgradeNode.of("follow", "Follow", 10).inTier(1).after(left);
        UpgradeTree tree = UpgradeTree.of(LONE, left, right, follow)
                .withFork(UpgradeCluster.of("Sides", left, right));

        UpgradeTreeLayout layout = layout(tree, state(left));

        UpgradeTreeLayout.Row one = layout.rows().stream().filter(row -> row.tier() == 1).findFirst().orElseThrow();
        assertThat(layout.spotOf(left).orElseThrow().y()).isBetween(one.bottom(), one.bandBottom());
        assertThat(layout.spotOf(follow).orElseThrow().x()).isGreaterThan(layout.split().orElseThrow());
    }

    @Test
    void aForkSideTooWideForItsColumnIsSqueezedToFitThePanel() {
        UpgradeNode left = UpgradeNode.of("side.l", "Left", 10).inTier(1);
        UpgradeNode right = UpgradeNode.of("side.r", "Right", 10).inTier(1);
        List<UpgradeNode> followers = IntStream.range(0, 8)
                .mapToObj(i -> UpgradeNode.of("follow." + i, "Follow " + i, 10).inTier(2).after(left)).toList();
        UpgradeTree tree = UpgradeTree.of(LONE, left, right).with(followers.toArray(UpgradeNode[]::new))
                .withFork(UpgradeCluster.of("Sides", left, right));

        UpgradeTreeLayout layout = layout(tree, state(left));

        assertThat(followers).allMatch(node -> layout.spotOf(node).isPresent());
        assertThat(layout.spots().values()).allMatch(spot -> spot.x() + spot.width() / 2 <= UpgradeTreeLayout.WIDTH);
    }

    private static UpgradeTreeLayout layout(UpgradeTree tree, UpgradeState owned) {
        return UpgradeTreeLayout.of(tree, owned, Optional.empty(), Set.of());
    }
}
