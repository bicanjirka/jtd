package td.ui;

import td.term.TermText;
import td.tower.NodeWords;
import td.tower.Tower;
import td.tower.upgrade.StandardBaseSlot;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeState;
import td.tower.upgrade.UpgradeTree;
import td.ui.render.IconFace;
import td.util.GameWorld;

import java.util.List;
import java.util.Optional;

/**
 * One node of the tree as its hover describes it, read once per refresh.
 *
 * @param xpProgress        progress toward its XP, e.g. {@code "XP 120/150"}
 * @param conditionProgress progress toward its gate condition, e.g. "1/2 nearby towers"
 * @param rivals            the other members of its exclusive choice still open, empty outside a choice
 * @param lastPick          whether buying it locks its rivals out
 * @param words             what it adds to the tower: its stat rows, then its sentences
 * @param face              the icon of the line it belongs to, at the level it brings the line to
 * @param addsTo            the name of the line's first node when this deepens a line, else empty
 * @param sealedBy          the Core step that keeps it closed, else empty
 * @param nextPickWaits     whether its choice's later picks wait for Transcendent, which the tower lacks
 */
record UpgradeOffer(UpgradeNode node, boolean xpMet, String xpProgress, boolean conditionMet,
                    TermText conditionProgress, List<UpgradeNode> rivals, boolean lastPick, NodeWords words,
                    IconFace face, Optional<String> addsTo, Optional<UpgradeNode> sealedBy, boolean nextPickWaits) {

    UpgradeOffer {
        rivals = List.copyOf(rivals);
    }

    /** {@code node} of {@code tower}'s tree, as it stands for the tower now. */
    static UpgradeOffer of(UpgradeNode node, Tower tower, GameWorld world) {
        UpgradeTree tree = tower.upgradeTree();
        UpgradeState owned = tower.upgrades();
        int level = node.tier().level();
        List<UpgradeNode> rivals = tree.choiceOf(node)
                .map(choice -> tree.nodes().stream().filter(other -> !other.equals(node) && choice.contains(other.id())
                        && !owned.owns(other.id()) && !tree.isLockedOut(other, owned)).toList())
                .orElse(List.of());
        return new UpgradeOffer(node, node.xpMet(tower), node.xpProgress(tower),
                node.gate().isSatisfied(tower, world), node.gate().progress(tower, world), rivals,
                tree.isLastPick(node, owned), tower.wordsOf(node),
                TermLooks.faceOfLine(tower.getType(), tree.artNodeOf(node), level),
                level > 1 ? Optional.of(tree.firstOf(node).displayName()) : Optional.empty(),
                tree.sealedBy(node, tower, world),
                !owned.owns(StandardBaseSlot.TRANSCENDENT_ID));
    }

    /** Whether it belongs to an exclusive choice with another member still open. */
    boolean inChoice() {
        return !this.rivals.isEmpty();
    }
}
