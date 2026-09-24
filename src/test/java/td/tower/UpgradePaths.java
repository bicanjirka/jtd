package td.tower;

import td.tower.upgrade.UpgradeNode;

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
}
