package td.ui;

import td.tower.Tower;
import td.tower.upgrade.UpgradeBonus;
import td.tower.upgrade.UpgradeNode;
import td.util.GameWorld;

import java.util.List;

/**
 * One offered upgrade node as the Upgrades panel shows it, read once per refresh.
 *
 * @param number            the key that buys it
 * @param xpProgress        progress toward its XP, e.g. {@code "XP 120/150"}
 * @param conditionProgress progress toward its gate condition, e.g. {@code "1/2 nearby towers"}
 * @param gateMet           its XP and gate condition both met (or ignored while playtesting)
 * @param rivals            the other members of its exclusive choice on offer, empty outside a choice
 * @param lastPick          whether buying it locks its rivals out for good
 * @param bonuses           what it would add to the tower: its data rows, then its perks' words
 */
record UpgradeOffer(UpgradeNode node, int number, boolean xpMet, String xpProgress, boolean conditionMet,
                    String conditionProgress, boolean gateMet, boolean affordable, List<UpgradeNode> rivals,
                    boolean lastPick, List<UpgradeBonus> bonuses) {

    UpgradeOffer {
        rivals = List.copyOf(rivals);
        bonuses = List.copyOf(bonuses);
    }

    /** {@code node}, one of the tower's {@code offered} nodes, numbered by the key that buys it. */
    static UpgradeOffer of(UpgradeNode node, List<UpgradeNode> offered, Tower tower, GameWorld world) {
        return new UpgradeOffer(node, tower.upgradeTree().keyOf(node), node.xpMet(tower), node.xpProgress(tower),
                node.gate().isSatisfied(tower, world), node.gate().progress(tower, world),
                node.gateMet(tower, world), world.economy().canPay(node.price()), tower.upgradeTree().rivalsOnOffer(node, offered),
                tower.upgradeTree().isLastPick(node, tower.upgrades()), tower.bonusesOf(node));
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
