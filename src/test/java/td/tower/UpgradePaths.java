package td.tower;

import td.damage.Damage;
import td.enemy.EnemyFactory;
import td.enemy.Rank;
import td.tower.upgrade.UpgradeNode;
import td.util.GameWorld;

/** Finds a tower's upgrade node by display name, so tests don't depend on list order. */
final class UpgradePaths {

    private UpgradePaths() {
    }

    static UpgradeNode named(Tower tower, String displayName) {
        return tower.upgradeTree().nodes().stream()
                .filter(n -> n.displayName().equals(displayName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No node named '" + displayName + "' on " + tower));
    }

    /** Buys Attune and then Awaken, which opens head levels I to III and the special slot. */
    static void awaken(Tower tower) {
        tower.buyUpgrade(named(tower, "Attune"));
        tower.buyUpgrade(named(tower, "Awaken"));
    }

    /**
     * Buys Attune and Awaken and then each named node, first clearing every kill and damage gate by killing
     * fodder. A cluster gate still needs the caller to place neighbours.
     */
    static void buy(AbstractTower tower, GameWorld world, String... names) {
        world.economy().startEconomy(1_000_000, 5);
        awaken(tower);
        for (int i = 0; i < 30; i++) {
            tower.dealDamage(EnemyFactory.getEnemy("c", world, 0, 2000, 1, Rank.GRUNT), Damage.physical(10_000_000));
        }
        for (String name : names) {
            if (!tower.buyUpgrade(named(tower, name))) {
                throw new IllegalStateException("could not buy " + name);
            }
        }
    }
}
