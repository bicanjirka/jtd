package td.ui;

import td.tower.Tower;
import td.tower.upgrade.UpgradeNode;
import td.util.GameWorld;

import java.util.List;

/**
 * One offered upgrade node as the Upgrades panel shows it, read once per refresh.
 *
 * @param number   the key that buys it
 * @param progress progress toward its gate, e.g. {@code "7/10 kills"}
 * @param rivals   the other members of its exclusive choice on offer, empty outside a choice
 * @param lastPick whether buying it locks its rivals out for good
 */
record UpgradeOffer(UpgradeNode node, int number, boolean gateMet, String progress, boolean affordable,
                    List<UpgradeNode> rivals, boolean lastPick) {

    UpgradeOffer {
        rivals = List.copyOf(rivals);
    }

    /** The {@code index}-th of the tower's {@code offered} nodes, numbered from one. */
    static UpgradeOffer of(List<UpgradeNode> offered, int index, Tower tower, GameWorld world) {
        UpgradeNode node = offered.get(index);
        return new UpgradeOffer(node, index + 1, node.gate().isSatisfied(tower, world), node.gate().progress(tower, world),
                world.economy().canPay(node.price()), tower.upgradeTree().rivalsOnOffer(node, offered),
                tower.upgradeTree().isLastPick(node, tower.upgrades()));
    }

    boolean buyable() {
        return this.gateMet && this.affordable;
    }

    /** Whether it belongs to an exclusive choice with another member on offer. */
    boolean inChoice() {
        return !this.rivals.isEmpty();
    }
}
