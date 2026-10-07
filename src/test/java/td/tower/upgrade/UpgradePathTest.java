package td.tower.upgrade;

import org.junit.jupiter.api.Test;
import td.fixtures.WorldFixtures;
import td.tower.AuraTower;
import td.tower.SniperTower;
import td.tower.Tower;
import td.util.GameWorld;

import static org.assertj.core.api.Assertions.assertThat;

class UpgradePathTest {

    private final GameWorld context = WorldFixtures.newWorld();

    private static UpgradeNode named(Tower tower, String name) {
        return tower.upgradeTree().nodes().stream().filter(node -> node.displayName().equals(name)).findFirst()
                .orElseThrow();
    }

    private static java.util.List<String> namesOf(java.util.List<UpgradeNode> path) {
        return path.stream().map(UpgradeNode::displayName).toList();
    }

    @Test
    void aBaseNodeWithNoPrerequisiteIsItsOwnPath() {
        SniperTower sniper = new SniperTower(this.context, 0, 0);

        assertThat(namesOf(sniper.upgradeTree().pathTo(named(sniper, "Attune")))).containsExactly("Attune");
    }

    @Test
    void aHeadLevelTwoComesAfterAttuneAndLevelOne() {
        SniperTower sniper = new SniperTower(this.context, 0, 0);

        assertThat(namesOf(sniper.upgradeTree().pathTo(named(sniper, "Focused Optics II"))))
                .containsExactly("Attune", "Focused Optics", "Focused Optics II");
    }

    @Test
    void aSpecialComesAfterAttuneAndAwakenWithoutNeedingTranscendent() {
        AuraTower aura = new AuraTower(this.context, 0, 0);

        assertThat(namesOf(aura.upgradeTree().pathTo(named(aura, "Rally")))).containsExactly("Attune", "Awaken", "Rally");
    }

    @Test
    void aSpecialThatFollowsAHeadLevelOneNeedsThatLevelToo() {
        SniperTower sniper = new SniperTower(this.context, 0, 0);

        assertThat(namesOf(sniper.upgradeTree().pathTo(named(sniper, "Momentum"))))
                .containsExactly("Attune", "Awaken", "Focused Optics", "Momentum");
    }

    @Test
    void theAurasThirdLevelOfEachChainLeadsThroughTheOthers() {
        AuraTower aura = new AuraTower(this.context, 0, 0);

        assertThat(namesOf(aura.upgradeTree().pathTo(named(aura, "Conduit"))))
                .containsExactly("Attune", "Broadcast", "Broadcast II", "Awaken", "Conduit");
    }

    @Test
    void everyNodeOfEveryTowersTreeHasAPathEndingInItself() {
        for (var type : td.tower.TowerFactory.Type.values()) {
            Tower tower = td.tower.TowerFactory.createTower(type, this.context, 0, 0);
            for (UpgradeNode node : tower.upgradeTree().nodes()) {
                assertThat(tower.upgradeTree().pathTo(node)).as("%s %s", type, node.displayName()).endsWith(node);
            }
        }
    }
}
