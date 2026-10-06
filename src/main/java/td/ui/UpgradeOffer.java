package td.ui;

import td.tower.Tower;
import td.tower.upgrade.UpgradeNode;
import td.util.GameWorld;

import java.util.List;

/**
 * One offered upgrade node as the Upgrades panel shows it, read once per refresh.
 *
 * @param number            the key that buys it
 * @param xpProgress        progress toward its XP, e.g. {@code "XP 120/150"}
 * @param conditionProgress progress toward its gate condition, e.g. {@code "1/2 nearby towers"}
 * @param rivals            the other members of its exclusive choice on offer, empty outside a choice
 * @param lastPick          whether buying it locks its rivals out for good
 */
record UpgradeOffer(UpgradeNode node, int number, boolean xpMet, String xpProgress, boolean conditionMet,
                    String conditionProgress, boolean affordable, List<UpgradeNode> rivals, boolean lastPick) {

    UpgradeOffer {
        rivals = List.copyOf(rivals);
    }

    /** The {@code index}-th of the tower's {@code offered} nodes, numbered from one. */
    static UpgradeOffer of(List<UpgradeNode> offered, int index, Tower tower, GameWorld world) {
        UpgradeNode node = offered.get(index);
        return new UpgradeOffer(node, index + 1, node.xpMet(tower), node.xpProgress(tower),
                node.gate().isSatisfied(tower, world), node.gate().progress(tower, world),
                world.economy().canPay(node.price()), tower.upgradeTree().rivalsOnOffer(node, offered),
                tower.upgradeTree().isLastPick(node, tower.upgrades()));
    }

    /** Its XP and its gate condition both met: price aside, it can be bought. */
    boolean gateMet() {
        return this.xpMet && this.conditionMet;
    }

    /** What still stands between the tower and this node, XP first. */
    String progress() {
        return this.xpMet ? this.conditionProgress : this.xpProgress;
    }

    boolean buyable() {
        return this.gateMet() && this.affordable;
    }

    /** Whether it belongs to an exclusive choice with another member on offer. */
    boolean inChoice() {
        return !this.rivals.isEmpty();
    }
}
