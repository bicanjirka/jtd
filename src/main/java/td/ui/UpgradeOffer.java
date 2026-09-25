package td.ui;

import td.tower.Tower;
import td.tower.upgrade.UpgradeNode;
import td.util.GameWorld;

/**
 * One offered upgrade node as the Upgrades panel shows it, read once per refresh.
 *
 * @param number   the key that buys it
 * @param progress progress toward its gate, e.g. {@code "7/10 kills"}
 */
record UpgradeOffer(UpgradeNode node, int number, boolean gateMet, String progress, boolean affordable) {

    static UpgradeOffer of(UpgradeNode node, int number, Tower tower, GameWorld world) {
        return new UpgradeOffer(node, number, node.gate().isSatisfied(tower, world), node.gate().progress(tower, world),
                world.economy().canPay(node.price()));
    }

    boolean buyable() {
        return this.gateMet && this.affordable;
    }
}
