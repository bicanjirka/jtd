package td.ui;

import org.junit.jupiter.api.Test;
import td.fixtures.WorldFixtures;
import td.tower.TowerFactory;
import td.tower.upgrade.UpgradeCluster;
import td.tower.upgrade.UpgradeCondition;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTree;
import td.ui.UpgradeTreeLayout.Link;
import td.ui.UpgradeTreeLayout.Point;
import td.ui.UpgradeTreeLayout.Spot;
import td.util.GameWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static td.ui.UpgradeTreePlan.cluster;
import static td.ui.UpgradeTreePlan.node;
import static td.ui.UpgradeTreePlan.row;
import static td.ui.UpgradeTreePlan.tier;

class UpgradeTreeLayoutTest {

    private static final UpgradeNode LEFT = UpgradeNode.of("left", "Left", 10);
    private static final UpgradeNode RIGHT = UpgradeNode.of("right", "Right", 10);
    private static final UpgradeNode SOLO = UpgradeNode.of("solo", "Solo", 8);
    private static final UpgradeNode EITHER = UpgradeNode.of("either", "Either", 12).inTier(1)
            .withRequires(UpgradeCondition.owns(LEFT.id()).or(UpgradeCondition.owns(RIGHT.id())));
    private static final UpgradeNode TWIN_A = UpgradeNode.of("twin.a", "Twin A", 15).inTier(1).after(SOLO);
    private static final UpgradeNode TWIN_B = UpgradeNode.of("twin.b", "Twin B", 15).inTier(1).after(SOLO);
    private static final UpgradeNode FAR = UpgradeNode.of("far", "Far", 20).inTier(2).after(SOLO);
    private static final UpgradeCluster PAIR = UpgradeCluster.of("Pair", LEFT, RIGHT);
    private static final UpgradeCluster TWINS = UpgradeCluster.of("Twins", TWIN_A, TWIN_B);
    private static final UpgradeTree TREE = UpgradeTree.of(LEFT, RIGHT, SOLO, EITHER, TWIN_A, TWIN_B, FAR)
            .withCluster(PAIR).withCluster(TWINS);
    private static final UpgradeTreePlan PLAN = UpgradeTreePlan.of(
            tier(row(cluster("Pair", 0f), node("solo", 3f))),
            tier(row(node("either", 0.5f), cluster("Twins", 3f))),
            tier(row(node("far", 3f))),
            tier());

    private static final UpgradeNode BY_LEFT = UpgradeNode.of("by.left", "By Left", 10).inTier(1).after(LEFT);
    private static final UpgradeNode BY_RIGHT = UpgradeNode.of("by.right", "By Right", 10).inTier(1).after(RIGHT);
    private static final UpgradeTree SIDES = UpgradeTree.of(LEFT, RIGHT, BY_LEFT, BY_RIGHT, SOLO).withCluster(PAIR);

    private final GameWorld context = WorldFixtures.newWorld();

    @Test
    void everyTowersPlanPlacesEachNodeOnceInsideThePanelWithNothingOverlapping() {
        for (TowerFactory.Type type : TowerFactory.Type.values()) {
            UpgradeTree tree = TowerFactory.createTower(type, this.context, 0, 0).upgradeTree();

            UpgradeTreeLayout layout = UpgradeTreeLayout.of(tree, UpgradeTreePlans.of(type));

            assertThat(layout.spots().keySet()).as(type.name()).containsExactlyInAnyOrderElementsOf(tree.nodes());
            assertThat(layout.spots().values()).as(type.name()).allMatch(spot -> spot.x() - spot.size() / 2
                    >= UpgradeTreeLayout.BODY_X && spot.x() + spot.size() / 2 <= UpgradeTreeLayout.WIDTH
                    - UpgradeTreeLayout.BAND_X);
            assertThat(layout.frames()).as(type.name()).allMatch(frame -> frame.box().left() >= UpgradeTreeLayout.BODY_X
                    && frame.box().right() <= UpgradeTreeLayout.WIDTH - UpgradeTreeLayout.BAND_X);
            assertThat(overlaps(layout)).as(type.name()).isEmpty();
            assertThat(layout.height()).as(type.name()).isLessThanOrEqualTo(UpgradeTreeLayout.HEIGHT);
        }
    }

    @Test
    void tiersClimbFromTheBottom() {
        UpgradeTreeLayout layout = UpgradeTreeLayout.of(TREE, PLAN);

        assertThat(layout.spotOf(EITHER).orElseThrow().y()).isLessThan(layout.spotOf(LEFT).orElseThrow().y());
        assertThat(layout.bandOf(1).bottom()).isLessThan(layout.bandOf(0).top());
    }

    @Test
    void aPlanThatLeavesANodeOutOrPlacesAMemberAloneIsRejected() {
        UpgradeTreePlan missing = UpgradeTreePlan.of(tier(row(cluster("Pair", 0f), node("solo", 3f))),
                tier(row(cluster("Twins", 3f))), tier(row(node("far", 3f))), tier());
        UpgradeTreePlan alone = UpgradeTreePlan.of(tier(row(node("left", 0f), node("right", 1f), node("solo", 3f))),
                tier(row(node("either", 0.5f), cluster("Twins", 3f))), tier(row(node("far", 3f))), tier());

        assertThatThrownBy(() -> UpgradeTreeLayout.of(TREE, missing)).hasMessageContaining("either");
        assertThatThrownBy(() -> UpgradeTreeLayout.of(TREE, alone)).hasMessageContaining("left");
    }

    @Test
    void aNeedOfAnyMemberHangsOffTheClustersFrameAndRunsOnlyAlongTheAxes() {
        UpgradeTreeLayout layout = UpgradeTreeLayout.of(TREE, PLAN);

        Link link = layout.links().stream().filter(l -> l.to().nodes().equals(List.of(EITHER))).findFirst()
                .orElseThrow();

        assertThat(link.from().cluster()).contains(PAIR);
        assertThat(link.path().getFirst().y()).isEqualTo(link.from().top());
        assertThat(link.path().getLast().y()).isEqualTo(link.to().bottom());
        assertThat(link.path().getLast().x()).isEqualTo(layout.spotOf(EITHER).orElseThrow().x());
        for (int i = 1; i < link.path().size(); i++) {
            Point a = link.path().get(i - 1);
            Point b = link.path().get(i);
            assertThat(a.x() == b.x() || a.y() == b.y()).isTrue();
        }
    }

    @Test
    void aClusterWhoseMembersShareANeedTakesOneLinkIntoItsFrame() {
        UpgradeTreeLayout layout = UpgradeTreeLayout.of(TREE, PLAN);

        assertThat(layout.links()).filteredOn(link -> link.from().nodes().equals(List.of(SOLO))).singleElement()
                .satisfies(link -> assertThat(link.to().cluster()).contains(TWINS));
    }

    @Test
    void aNeedMoreThanATierBelowIsABadgeOnTheNodesCornerRatherThanALink() {
        UpgradeTreeLayout layout = UpgradeTreeLayout.of(TREE, PLAN);
        Spot far = layout.spotOf(FAR).orElseThrow();

        assertThat(layout.links()).noneMatch(link -> link.to().nodes().contains(FAR));
        assertThat(layout.badges()).singleElement().satisfies(badge -> {
            assertThat(badge.from().nodes()).containsExactly(SOLO);
            assertThat(badge.x()).isLessThan(far.x());
            assertThat(badge.y()).isLessThan(far.y());
        });
    }

    @Test
    void leftToRightOrdersNodesByWhereTheySit() {
        UpgradeTreeLayout layout = UpgradeTreeLayout.of(TREE, PLAN);

        assertThat(layout.leftToRight(List.of(SOLO, RIGHT, LEFT))).containsExactly(LEFT, RIGHT, SOLO);
    }

    @Test
    void nodesBehindDifferentMembersOfOneClusterShareASlotThatShowsThePickedOrHoveredSide() {
        UpgradeTreeLayout layout = UpgradeTreeLayout.of(SIDES, UpgradeTreePlan.of(
                tier(row(cluster("Pair", 0f), node("solo", 3f))),
                tier(row(node("by.left", 1f), node("by.right", 1f))), tier(), tier()));

        UpgradeTreeLayout.Showing none = layout.showing(_ -> false, Optional.empty());
        UpgradeTreeLayout.Showing picked = layout.showing(RIGHT::equals, Optional.empty());
        UpgradeTreeLayout.Showing previewed = layout.showing(RIGHT::equals, Optional.of(LEFT));

        assertThat(layout.shared()).singleElement().satisfies(slot -> assertThat(slot.cluster()).isEqualTo(PAIR));
        assertThat(none.hidden()).containsExactlyInAnyOrder(BY_LEFT, BY_RIGHT);
        assertThat(none.empty()).hasSize(1);
        assertThat(picked.hidden()).containsExactly(BY_LEFT);
        assertThat(picked.empty()).isEmpty();
        assertThat(previewed.hidden()).containsExactly(BY_RIGHT);
    }

    @Test
    void nodesNotBehindDifferentMembersOfOneClusterMayNotShareASlot() {
        UpgradeTreePlan onAMember = UpgradeTreePlan.of(tier(row(cluster("Pair", 0f), node("solo", 0f))),
                tier(row(node("by.left", 1f), node("by.right", 2f))), tier(), tier());

        assertThatThrownBy(() -> UpgradeTreeLayout.of(SIDES, onAMember)).hasMessageContaining("share a slot");
    }

    /**
     * Every pair of nodes that touch, and every node a frame it isn't in covers, but for the nodes of a shared slot,
     * which only ever show one at a time.
     */
    private static List<String> overlaps(UpgradeTreeLayout layout) {
        List<String> found = new ArrayList<>();
        List<Map.Entry<UpgradeNode, Spot>> spots = List.copyOf(layout.spots().entrySet());
        for (int i = 0; i < spots.size(); i++) {
            Spot a = spots.get(i).getValue();
            UpgradeNode node = spots.get(i).getKey();
            for (int j = i + 1; j < spots.size(); j++) {
                Spot b = spots.get(j).getValue();
                UpgradeNode other = spots.get(j).getKey();
                boolean shared = layout.shared().stream().anyMatch(slot -> slot.bySide().containsValue(node)
                        && slot.bySide().containsValue(other));
                if (!shared && Math.abs(a.x() - b.x()) < UpgradeTreeLayout.NODE
                        && Math.abs(a.y() - b.y()) < UpgradeTreeLayout.NODE) {
                    found.add(node.id() + " / " + other.id());
                }
            }
            for (UpgradeTreeLayout.Frame frame : layout.frames()) {
                boolean member = frame.cluster().contains(node.id()) || layout.shared().stream()
                        .anyMatch(slot -> slot.bySide().containsValue(node) && slot.bySide().values().stream()
                                .anyMatch(sharer -> frame.cluster().contains(sharer.id())));
                if (!member && a.x() + a.size() / 2 > frame.box().left() && a.x() - a.size() / 2 < frame.box().right()
                        && a.y() + a.size() / 2 > frame.box().top() && a.y() - a.size() / 2 < frame.box().bottom()) {
                    found.add(spots.get(i).getKey().id() + " in " + frame.cluster().name());
                }
            }
        }
        return found;
    }
}
