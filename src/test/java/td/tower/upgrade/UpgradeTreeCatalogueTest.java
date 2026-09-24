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
    void everyTowerOffersABaseRangeAndAwakenNodeWithNoPrerequisite() {
        for (TowerFactory.Type type : TowerFactory.Type.values()) {
            Tower tower = TowerFactory.createTower(type, this.context, 0, 0);

            List<UpgradeNode> offered = tower.offeredUpgrades(this.context);

            assertThat(offered).as("%s's initially offered nodes", type).extracting(UpgradeNode::id)
                    .contains(StandardBaseSlot.RANGE_ID, StandardBaseSlot.AWAKEN_ID);
        }
    }

    @Test
    void noHeadOrSpecialNodeIsOfferedBeforeAwakenIsOwned() {
        for (TowerFactory.Type type : TowerFactory.Type.values()) {
            Tower tower = TowerFactory.createTower(type, this.context, 0, 0);

            List<UpgradeNode> offered = tower.offeredUpgrades(this.context);

            assertThat(offered).as("%s's initially offered nodes", type).allSatisfy(node ->
                    assertThat(node.slot()).as("%s's node %s offered before Awaken", type, node.id())
                            .isEqualTo(UpgradeSlot.BASE));
        }
    }
}
