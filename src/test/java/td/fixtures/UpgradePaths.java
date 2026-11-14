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
     * Buys each named node with every node its tree puts before it, the unlocks of its tier included, on credits the
     * world is given for it. XP is waived for the whole world from here on.
     */
    public static void buy(Tower tower, GameWorld world, String... names) {
        world.playtestRules().setUpgradeGatesIgnored(true);
        world.economy().startEconomy(1_000_000, 5);
        for (String name : names) {
            UpgradeNode node = named(tower, name);
            if (!tower.upgrades().owns(node.id()) && !tower.buyPath(node)) {
                throw new IllegalStateException("could not buy " + name + " on " + tower);
            }
        }
    }
}
