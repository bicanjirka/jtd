package td.tower.upgrade;

import org.junit.jupiter.api.Test;
import td.fixtures.WorldFixtures;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.util.GameWorld;

import static org.assertj.core.api.Assertions.assertThat;
import static td.fixtures.TierTrees.A1;
import static td.fixtures.TierTrees.A2;
import static td.fixtures.TierTrees.LONE;
import static td.fixtures.TierTrees.MID;
import static td.fixtures.TierTrees.S1;
import static td.fixtures.TierTrees.TIER_ONE;
import static td.fixtures.TierTrees.TREE;

class UpgradePathTest {

    private final GameWorld context = WorldFixtures.newWorld();

    @Test
    void aTierZeroNodeWithNoPrerequisiteIsItsOwnPath() {
        assertThat(TREE.pathTo(LONE)).containsExactly(LONE);
    }

    @Test
    void aLoneNodeInAPaidTierComesAfterTheTiersUnlock() {
        assertThat(TREE.pathTo(MID)).containsExactly(TIER_ONE, MID);
    }

    @Test
    void aNodeInAFreeTierNeedsOnlyThePaidUnlocksBelowIt() {
        assertThat(TREE.pathTo(S1)).containsExactly(TIER_ONE, S1);
    }

    @Test
    void aLinesStepComesAfterTheStepBeforeIt() {
        assertThat(TREE.pathTo(A2)).containsExactly(TIER_ONE, A1, A2);
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
