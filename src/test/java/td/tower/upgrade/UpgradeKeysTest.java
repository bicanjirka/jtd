package td.tower.upgrade;

import org.junit.jupiter.api.Test;
import td.fixtures.WorldFixtures;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.util.GameWorld;
import td.util.RandomSource;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UpgradeKeysTest {

    private static UpgradeTree treeOf(TowerFactory.Type type) {
        GameWorld world = WorldFixtures.newWorld();
        return TowerFactory.createTower(type, world, 0, 0).upgradeTree();
    }

    private static int key(UpgradeTree tree, String id) {
        return tree.keyOf(tree.nodes().stream().filter(node -> node.id().equals(id)).findFirst().orElseThrow());
    }

    @Test
    void theCoreChainIsOneAndTheRangeLineTwoOnEveryTower() {
        for (TowerFactory.Type type : TowerFactory.Type.values()) {
            UpgradeTree tree = treeOf(type);

            assertThat(tree.nodesIn(UpgradeSlot.CORE)).allSatisfy(node -> assertThat(tree.keyOf(node)).isEqualTo(1));
            assertThat(tree.nodesIn(UpgradeSlot.BASE)).allSatisfy(node -> assertThat(tree.keyOf(node)).isEqualTo(2));
        }
    }

    @Test
    void theLineThatDoesNotBranchIsThreeAndTheTwoThatDoAreFourAndFive() {
        UpgradeTree burst = treeOf(TowerFactory.Type.SPLASH);

        assertThat(key(burst, "splash.extra.blast_engineering.1")).isEqualTo(3);
        assertThat(key(burst, "splash.extra.blast_engineering.3")).isEqualTo(3);
        assertThat(key(burst, "splash.head.arc.1")).isEqualTo(4);
        assertThat(key(burst, "splash.head.arc.3")).isEqualTo(4);
        assertThat(key(burst, "splash.head.hex.2")).isEqualTo(5);
        assertThat(key(burst, "splash.head.arc.4a")).isEqualTo(4);
        assertThat(key(burst, "splash.head.arc.4b")).isEqualTo(5);
    }

    @Test
    void eachSetOfSpecialsANodeOpensIsSixToEight() {
        UpgradeTree sniper = treeOf(TowerFactory.Type.SNIPER);

        assertThat(List.of(key(sniper, "sniper.special.momentum"), key(sniper, "sniper.special.ricochet"),
                key(sniper, "sniper.special.headhunter"))).containsExactly(6, 7, 8);
        assertThat(List.of(key(sniper, "sniper.special.hollow_point"), key(sniper, "sniper.special.fifth_shot"),
                key(sniper, "sniper.special.shatter_shot"))).containsExactly(6, 7, 8);
    }

    @Test
    void whateverATowerBuysEveryNodeOnOfferHasAKeyOfItsOwnAndNoneIsNine() {
        RandomSource random = RandomSource.seeded(3);
        for (TowerFactory.Type type : TowerFactory.Type.values()) {
            for (int walk = 0; walk < 20; walk++) {
                GameWorld world = WorldFixtures.newWorld();
                world.playtestRules().setUpgradeGatesIgnored(true);
                world.economy().startEconomy(1_000_000, 5);
                Tower tower = TowerFactory.createTower(type, world, 0, 0);
                List<UpgradeNode> offered = tower.offeredUpgrades(world);
                while (!offered.isEmpty()) {
                    List<Integer> keys = offered.stream().map(tower.upgradeTree()::keyOf).toList();

                    assertThat(keys).as(type + " offers " + offered.stream().map(UpgradeNode::id)
                            .collect(Collectors.joining(", "))).doesNotHaveDuplicates().allMatch(key -> key >= 1 && key <= 8);

                    tower.buyUpgrade(offered.get(random.nextIndex(offered.size())));
                    offered = tower.offeredUpgrades(world);
                }
            }
        }
    }

    @Test
    void aTreeWithANodeNoNumberKeyBuysIsRejected() {
        UpgradeNode first = UpgradeNode.of("a", UpgradeSlot.HEAD, "A", 10);
        UpgradeNode second = UpgradeNode.of("b", UpgradeSlot.HEAD, "B", 10);
        UpgradeNode third = UpgradeNode.of("c", UpgradeSlot.HEAD, "C", 10);

        assertThatThrownBy(() -> UpgradeTree.of(first, second, third)
                .withChoice(ExclusiveChoice.oneOf(first, second, third)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageEndingWith(": c");
    }
}
