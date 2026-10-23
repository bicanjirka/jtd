package td.fixtures;

import td.tower.Tower;
import td.tower.upgrade.UpgradeNode;
import td.util.GameWorld;

/**
 * Finds and buys a tower's upgrade nodes by display name, so a test names the node it is about and
 * not the tree around it.
 */
public final class UpgradePaths {

    private UpgradePaths() {
    }

    public static UpgradeNode named(Tower tower, String displayName) {
        return tower.upgradeTree().nodes().stream()
                .filter(node -> node.displayName().equals(displayName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No node named '" + displayName + "' on " + tower));
    }

    /**
     * Grants more XP than any node needs, then buys Attune and Awaken, which open head levels I to
     * III and the special slot; gates still hold.
     */
    public static void awakenVeteran(Tower tower) {
        tower.earnXp(1_000);
        tower.buyUpgrade(named(tower, "Attune"));
        tower.buyUpgrade(named(tower, "Awaken"));
    }

    /**
     * Buys each named node and every node its tree puts before it, on credits the world is given for
     * it. XP and gates are waived for the whole world from here on.
     */
    public static void buy(Tower tower, GameWorld world, String... names) {
        world.playtestRules().setUpgradeGatesIgnored(true);
        world.economy().startEconomy(1_000_000, 5);
        for (String name : names) {
            buyWithWhatItNeeds(tower, world, named(tower, name));
        }
    }

    /** Only what is still missing: a node that can follow either of two owns whichever is already there. */
    private static void buyWithWhatItNeeds(Tower tower, GameWorld world, UpgradeNode node) {
        if (tower.upgrades().owns(node.id())) {
            return;
        }
        if (!node.requires().isSatisfied(tower, world)) {
            node.requires().requiredNodes().stream().sorted()
                    .forEach(id -> buyWithWhatItNeeds(tower, world, withId(tower, id)));
        }
        if (!tower.buyUpgrade(node)) {
            throw new IllegalStateException("could not buy " + node.displayName() + " on " + tower);
        }
    }

    private static UpgradeNode withId(Tower tower, String id) {
        return tower.upgradeTree().nodes().stream().filter(node -> node.id().equals(id)).findFirst().orElseThrow();
    }
}
