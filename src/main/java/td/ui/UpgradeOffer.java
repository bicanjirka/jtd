package td.ui;

import td.term.TermText;
import td.tower.NodeWords;
import td.tower.Tower;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeTree;
import td.ui.render.IconFace;
import td.util.GameWorld;

import java.util.List;
import java.util.Optional;

/**
 * One offered upgrade node as the Upgrades panel shows it, read once per refresh.
 *
 * @param number            the key that buys it
 * @param xpProgress        progress toward its XP, e.g. {@code "XP 120/150"}
 * @param conditionProgress progress toward its gate condition, e.g. "1/2 nearby towers"
 * @param gateMet           its XP and gate condition both met (or ignored while playtesting)
 * @param rivals            the other members of its exclusive choice on offer, empty outside a choice
 * @param lastPick          whether buying it locks its rivals out for good
 * @param words             what it would add to the tower: its stat rows, then its sentences
 * @param face              the icon of the line it belongs to, at the level it would bring the line to
 * @param addsTo            the name of the line's first node when this deepens a line, else empty
 */
record UpgradeOffer(UpgradeNode node, int number, boolean xpMet, String xpProgress, boolean conditionMet,
                    TermText conditionProgress, boolean gateMet, boolean affordable, List<UpgradeNode> rivals,
                    boolean lastPick, NodeWords words, IconFace face, Optional<String> addsTo) {

    UpgradeOffer {
        rivals = List.copyOf(rivals);
    }

    /** {@code node}, one of the tower's {@code offered} nodes, numbered by the key that buys it. */
    static UpgradeOffer of(UpgradeNode node, List<UpgradeNode> offered, Tower tower, GameWorld world) {
        UpgradeTree tree = tower.upgradeTree();
        int level = node.tier().level();
        return new UpgradeOffer(node, tree.keyOf(node), node.xpMet(tower), node.xpProgress(tower),
                node.gate().isSatisfied(tower, world), node.gate().progress(tower, world),
                node.gateMet(tower, world), world.economy().canPay(node.price()), tree.rivalsOnOffer(node, offered),
                tree.isLastPick(node, tower.upgrades()), tower.wordsOf(node),
                TermLooks.faceOfLine(tower.getType(), tree.artNodeOf(node), level),
                level > 1 ? Optional.of(tree.firstOf(node).displayName()) : Optional.empty());
    }

    /** What still stands between the tower and this node, XP first. */
    TermText progress() {
        return this.xpMet ? this.conditionProgress : TermText.of(this.xpProgress);
    }

    boolean buyable() {
        return this.gateMet() && this.affordable;
    }

    /** Whether it belongs to an exclusive choice with another member on offer. */
    boolean inChoice() {
        return !this.rivals.isEmpty();
    }
}
