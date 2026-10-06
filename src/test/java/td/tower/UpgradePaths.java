package td.tower;

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

    /**
     * Grants more XP than any node needs, then buys Attune and Awaken, which open head levels I to
     * III and the special slot.
     */
    static void awakenVeteran(Tower tower) {
        tower.earnXp(1_000);
        tower.buyUpgrade(named(tower, "Attune"));
        tower.buyUpgrade(named(tower, "Awaken"));
    }

    /**
     * Grants XP, buys Attune and Awaken and then each named node. A cluster gate still needs the
     * caller to place neighbours.
     */
    static void buy(AbstractTower tower, GameWorld world, String... names) {
        world.economy().startEconomy(1_000_000, 5);
        awakenVeteran(tower);
        for (String name : names) {
            if (!tower.buyUpgrade(named(tower, name))) {
                throw new IllegalStateException("could not buy " + name);
            }
        }
    }
}
