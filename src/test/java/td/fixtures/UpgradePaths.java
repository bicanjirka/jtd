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
     * Buys each named node and every node its tree puts before it, on credits the world is given for
     * it. XP and gates are waived for the whole world from here on.
     */
    public static void buy(Tower tower, GameWorld world, String... names) {
        world.playtestRules().setUpgradeGatesIgnored(true);
        world.economy().startEconomy(1_000_000, 5);
        for (String name : names) {
            if (!buyWithWhatItNeeds(tower, world, named(tower, name))) {
                throw new IllegalStateException("could not buy " + name + " on " + tower);
            }
        }
    }

    /**
     * Only what is still missing. A prerequisite that cannot be bought is one side of an either-or the
     * tower already took the other way, so only the node asked for has to succeed.
     */
    private static boolean buyWithWhatItNeeds(Tower tower, GameWorld world, UpgradeNode node) {
        if (tower.upgrades().owns(node.id())) {
            return true;
        }
        if (!node.requires().isSatisfied(tower, world)) {
            node.requires().requiredNodes().stream().sorted()
                    .forEach(id -> buyWithWhatItNeeds(tower, world, withId(tower, id)));
        }
        return tower.buyUpgrade(node);
    }

    private static UpgradeNode withId(Tower tower, String id) {
        return tower.upgradeTree().nodes().stream().filter(node -> node.id().equals(id)).findFirst().orElseThrow();
    }
}
