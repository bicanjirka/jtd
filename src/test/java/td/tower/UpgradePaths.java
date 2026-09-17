package td.tower;

import td.tower.upgrade.UpgradePath;

/**
 * Looks up one of a tower's own upgrade paths by display name, so tests don't depend on list order.
 */
final class UpgradePaths {

    private UpgradePaths() {
    }

    static UpgradePath named(Tower tower, String displayName) {
        return tower.availablePaths().stream()
                .filter(p -> p.displayName().equals(displayName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No path named '" + displayName + "' on " + tower));
    }
}
