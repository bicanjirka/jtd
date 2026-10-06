package td.tower.upgrade;

import org.junit.jupiter.api.Test;
import td.fixtures.FakeTower;
import td.fixtures.WorldFixtures;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.util.GameWorld;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Invariants every real tower's tree must hold. */
class UpgradeTreeCatalogueTest {

    private final GameWorld context = WorldFixtures.newWorld();

    @Test
    void everyTowersTreeHasUniqueNodeIds() {
        for (TowerFactory.Type type : TowerFactory.Type.values()) {
            Tower tower = TowerFactory.createTower(type, this.context, 0, 0);

            List<UpgradeNode> nodes = tower.upgradeTree().nodes();
            Set<String> ids = new HashSet<>();
            for (UpgradeNode node : nodes) {
                assertThat(ids.add(node.id())).as("duplicate id %s in %s's tree", node.id(), type).isTrue();
            }
        }
    }

    @Test
    void aFreshTowerOfEveryTypeIsOfferedRangeAndAttuneOnly() {
        for (TowerFactory.Type type : TowerFactory.Type.values()) {
            Tower tower = TowerFactory.createTower(type, this.context, 0, 0);

            List<UpgradeNode> offered = tower.offeredUpgrades(this.context);

            assertThat(offered).as("%s's initially offered nodes", type).extracting(UpgradeNode::id)
                    .containsExactly(StandardBaseSlot.RANGE_ID, StandardBaseSlot.ATTUNE_ID);
        }
    }

    @Test
    void attuneOpensEveryTowersHeadButNoSpecial() {
        for (TowerFactory.Type type : TowerFactory.Type.values()) {
            UpgradeTree tree = TowerFactory.createTower(type, this.context, 0, 0).upgradeTree();

            List<UpgradeNode> offered = tree.offered(this.owning(tree, Set.of(StandardBaseSlot.ATTUNE_ID)), this.context);

            assertThat(offered).as("%s after Attune", type).anyMatch(node -> node.slot() == UpgradeSlot.HEAD)
                    .noneMatch(node -> node.slot() == UpgradeSlot.SPECIAL);
        }
    }

    @Test
    void choosingAHeadRootLocksOutEveryOtherRootOnEveryTower() {
        for (TowerFactory.Type type : TowerFactory.Type.values()) {
            UpgradeTree tree = TowerFactory.createTower(type, this.context, 0, 0).upgradeTree();
            List<UpgradeNode> roots = tree.offered(this.owning(tree, Set.of(StandardBaseSlot.ATTUNE_ID)), this.context)
                    .stream().filter(node -> node.slot() == UpgradeSlot.HEAD && tree.choiceOf(node).isPresent()).toList();

            for (UpgradeNode root : roots) {
                List<UpgradeNode> offered = tree.offered(
                        this.owning(tree, Set.of(StandardBaseSlot.ATTUNE_ID, root.id())), this.context);

                assertThat(offered).as("%s after %s", type, root.id()).doesNotContainAnyElementsOf(
                        roots.stream().filter(other -> !other.equals(root)).toList());
            }
        }
    }

    /** The panel shows three buttons per slot, and number keys count what it shows. */
    @Test
    void noTowerEverOffersMoreThanThreeNodesInOneSlot() {
        for (TowerFactory.Type type : TowerFactory.Type.values()) {
            UpgradeTree tree = TowerFactory.createTower(type, this.context, 0, 0).upgradeTree();
            Set<Set<String>> seen = new HashSet<>();
            Deque<Set<String>> pending = new ArrayDeque<>(List.of(Set.of()));
            while (!pending.isEmpty()) {
                Set<String> owned = pending.pop();
                if (!seen.add(owned)) {
                    continue;
                }
                List<UpgradeNode> offered = tree.offered(this.owning(tree, owned), this.context);
                for (UpgradeSlot slot : UpgradeSlot.values()) {
                    assertThat(offered.stream().filter(node -> node.slot() == slot))
                            .as("%s owning %s offers in %s", type, owned, slot).hasSizeLessThanOrEqualTo(3);
                }
                for (UpgradeNode node : offered) {
                    Set<String> next = new HashSet<>(owned);
                    next.add(node.id());
                    pending.push(Set.copyOf(next));
                }
            }
        }
    }

    /** Reports {@code ownedIds} as owned, gates aside, so every reachable state can be walked. */
    private Tower owning(UpgradeTree tree, Set<String> ownedIds) {
        List<UpgradeNode> owned = tree.nodes().stream().filter(node -> ownedIds.contains(node.id())).toList();
        return FakeTower.owning(this.context, 0, 0, new UpgradeState(owned));
    }
}
