package td.tower.upgrade;

import org.junit.jupiter.api.Test;
import td.fixtures.WorldFixtures;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.util.GameWorld;
import td.util.RandomSource;

import java.util.List;
import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;
import static td.fixtures.TierTrees.A1;
import static td.fixtures.TierTrees.A2;
import static td.fixtures.TierTrees.B1;
import static td.fixtures.TierTrees.B2;
import static td.fixtures.TierTrees.LONE;
import static td.fixtures.TierTrees.MID;
import static td.fixtures.TierTrees.TIER_ONE;
import static td.fixtures.TierTrees.TREE;
import static td.fixtures.TierTrees.towerOwning;

class UpgradeKeysTest {

    private final GameWorld context = WorldFixtures.newWorld();

    @Test
    void keysNumberTheHighestOpenTierLeftToRightAndAClusterTakesOneKeyForItsFirstMember() {
        Tower tower = towerOwning(this.context, 0);

        assertThat(TREE.keyed(tower, this.context)).containsExactly(A1, LONE);
        assertThat(TREE.keyOf(B1, tower, this.context)).hasValue(1);
        assertThat(TREE.keyOf(LONE, tower, this.context)).hasValue(2);
    }

    @Test
    void onceATierAboveOpensTheLowerTiersAreBoughtWithTheMouse() {
        Tower tower = towerOwning(this.context, UpgradeTier.standard().get(1).xp(), A1, TIER_ONE);

        assertThat(TREE.keyed(tower, this.context)).containsExactly(A2, MID);
        assertThat(TREE.keyOf(LONE, tower, this.context)).isEmpty();
    }

    @Test
    void aBranchTakesKeysOnlyForTheMemberTheTowerOwns() {
        Tower tower = towerOwning(this.context, UpgradeTier.standard().get(1).xp(), B1, TIER_ONE);

        assertThat(TREE.keyOf(B2, tower, this.context)).hasValue(1);
        assertThat(TREE.keyOf(A2, tower, this.context)).isEmpty();
    }

    @Test
    void whateverATowerBuysEveryNodeOfferedInItsHighestOpenTierHasAKey() {
        RandomSource random = RandomSource.seeded(3);
        for (TowerFactory.Type type : TowerFactory.Type.values()) {
            for (int walk = 0; walk < 10; walk++) {
                GameWorld world = WorldFixtures.newWorld();
                world.playtestRules().setUpgradeGatesIgnored(true);
                world.economy().startEconomy(1_000_000, 5);
                Tower tower = TowerFactory.createTower(type, world, 0, 0);
                UpgradeTree tree = tower.upgradeTree();
                List<UpgradeNode> offered = tower.offeredUpgrades(world);
                while (!offered.isEmpty()) {
                    int top = tree.highestOpenTier(tower, world);
                    for (UpgradeNode node : offered) {
                        OptionalInt key = tree.keyOf(node, tower, world);
                        if (node.tier() == top && !tree.isUnlock(node)) {
                            assertThat(key).as("%s %s", type, node.id()).isPresent();
                        }
                    }
                    tower.buyUpgrade(offered.get(random.nextIndex(offered.size())));
                    offered = tower.offeredUpgrades(world);
                }
            }
        }
    }
}
