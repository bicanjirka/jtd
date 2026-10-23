package td.tower.upgrade;

import org.junit.jupiter.api.Test;
import td.fixtures.WorldFixtures;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.util.GameWorld;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UpgradePathTest {

    private static final UpgradeNode HEAD = UpgradeTier.HEAD_1.node("test.head.1", "Head", 10);
    private static final UpgradeNode HEAD_TWO = UpgradeTier.HEAD_2.node("test.head.2", "Head II", 10).after(HEAD);
    private static final UpgradeNode HEAD_THREE = UpgradeTier.HEAD_3.node("test.head.3", "Head III", 10).after(HEAD_TWO);
    private static final UpgradeNode SPECIAL = UpgradeTier.SPECIAL.node("test.special.free", "Free Special", 10);
    private static final UpgradeNode FOLLOWING = UpgradeTier.SPECIAL.node("test.special.following", "Following Special", 10)
            .after(HEAD);
    private static final UpgradeTree TREE = UpgradeTree.of(StandardBaseSlot.nodes(10, 3f, HEAD_THREE))
            .with(HEAD, HEAD_TWO, HEAD_THREE, SPECIAL, FOLLOWING);

    private final GameWorld context = WorldFixtures.newWorld();

    private static List<String> pathTo(UpgradeNode node) {
        return TREE.pathTo(node).stream().map(UpgradeNode::displayName).toList();
    }

    private static UpgradeNode base(String name) {
        return TREE.nodes().stream().filter(node -> node.displayName().equals(name)).findFirst().orElseThrow();
    }

    @Test
    void aBaseNodeWithNoPrerequisiteIsItsOwnPath() {
        assertThat(pathTo(base("Attune"))).containsExactly("Attune");
    }

    @Test
    void aHeadLevelTwoComesAfterAttuneAndLevelOne() {
        assertThat(pathTo(HEAD_TWO)).containsExactly("Attune", "Head", "Head II");
    }

    @Test
    void aSpecialComesAfterAttuneAndAwakenWithoutNeedingTranscendent() {
        assertThat(pathTo(SPECIAL)).containsExactly("Attune", "Awaken", "Free Special");
    }

    @Test
    void aSpecialThatFollowsAHeadLevelOneNeedsThatLevelToo() {
        assertThat(pathTo(FOLLOWING)).containsExactly("Attune", "Awaken", "Head", "Following Special");
    }

    @Test
    void aHeadLevelThreeLeadsThroughItsChainAndAwaken() {
        assertThat(pathTo(HEAD_THREE)).containsExactly("Attune", "Awaken", "Head", "Head II", "Head III");
    }

    @Test
    void everyNodeOfEveryTowersTreeHasAPathEndingInItself() {
        for (TowerFactory.Type type : TowerFactory.Type.values()) {
            Tower tower = TowerFactory.createTower(type, this.context, 0, 0);
            for (UpgradeNode node : tower.upgradeTree().nodes()) {
                assertThat(tower.upgradeTree().pathTo(node)).as("%s %s", type, node.displayName()).endsWith(node);
            }
        }
    }
}
