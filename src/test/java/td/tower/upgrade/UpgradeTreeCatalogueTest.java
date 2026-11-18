package td.tower.upgrade;

import org.junit.jupiter.api.Test;
import td.fixtures.WorldFixtures;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.util.GameWorld;

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
    void aFreshTowerOfEveryTypeIsOfferedOnlyTierZero() {
        for (TowerFactory.Type type : TowerFactory.Type.values()) {
            Tower tower = TowerFactory.createTower(type, this.context, 0, 0);

            List<UpgradeNode> offered = tower.offeredUpgrades(this.context);

            assertThat(offered).as("%s's initially offered nodes", type).isNotEmpty()
                    .allMatch(node -> node.tier() == 0);
        }
    }

    @Test
    void everyTowerHasAnUpgradeInEveryTier() {
        for (TowerFactory.Type type : TowerFactory.Type.values()) {
            UpgradeTree tree = TowerFactory.createTower(type, this.context, 0, 0).upgradeTree();

            for (int tier = 0; tier < tree.tiers().size(); tier++) {
                int t = tier;
                assertThat(tree.nodes()).as("%s's tier %s", type, tier).anyMatch(node -> node.tier() == t);
            }
        }
    }

    @Test
    void everyTowersTiersOpenOnXpAlone() {
        for (TowerFactory.Type type : TowerFactory.Type.values()) {
            UpgradeTree tree = TowerFactory.createTower(type, this.context, 0, 0).upgradeTree();

            assertThat(tree.tiers()).as("%s's tiers", type).noneMatch(UpgradeTier::isPaid);
        }
    }

    @Test
    void pickingAClusterMemberLocksOutTheOthersOnEveryTower() {
        for (TowerFactory.Type type : TowerFactory.Type.values()) {
            UpgradeTree tree = TowerFactory.createTower(type, this.context, 0, 0).upgradeTree();
            for (UpgradeCluster cluster : tree.clusters()) {
                UpgradeNode picked = tree.node(cluster.memberIds().getFirst()).orElseThrow();
                UpgradeState owned = UpgradeState.none().with(picked);

                assertThat(cluster.memberIds().subList(1, cluster.memberIds().size()))
                        .as("%s's %s", type, cluster.name())
                        .allMatch(id -> tree.isLockedOut(tree.node(id).orElseThrow(), owned));
            }
        }
    }
}
