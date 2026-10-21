package td.ui;

import td.term.TermText;
import td.tower.TowerRank;
import td.tower.upgrade.StandardBaseSlot;
import td.tower.upgrade.UpgradeCondition;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeSlot;
import td.tower.upgrade.UpgradeState;
import td.ui.render.InfoSheet;
import td.ui.render.Palette;
import td.ui.render.SheetLine;
import td.ui.render.SheetLine.Glyph;
import td.ui.render.SheetLine.Row;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** The words for an upgrade node: its hover sheet, its button face and its slot's header. Pure. */
final class UpgradeSheetText {

    private UpgradeSheetText() {
    }

    /**
     * The node's face, name and price; a ✔/✘ line for its XP and one for its gate; what it locks out; then what it
     * adds: its value rows and its sentences.
     */
    static InfoSheet hover(UpgradeOffer offer) {
        UpgradeNode node = offer.node();
        List<SheetLine> lines = new ArrayList<>();
        lines.add(new SheetLine.IconTitle(offer.face(), node.displayName(), offer.face().tone(), "$" + node.price()));
        if (node.xp() > 0) {
            lines.add(gate(offer.xpMet(), TermText.of(offer.xpProgress())));
        }
        if (!node.gate().equals(UpgradeCondition.always())) {
            lines.add(gate(offer.conditionMet(), offer.conditionProgress()));
        }
        // The rivals get rows of their own: "Locks out" and a long name don't fit one line together.
        if (offer.inChoice() && offer.lastPick()) {
            lines.add(Row.plain(Glyph.LOCK, "Locks out", ""));
            offer.rivals().forEach(rival -> lines.add(Row.plain(Glyph.DOT, rival.displayName(), "")));
        } else if (offer.inChoice()) {
            lines.add(Row.plain(Glyph.LOCK, "Next pick", "Transcendent"));
        }
        lines.add(new SheetLine.Gap());
        offer.addsTo().ifPresent(first -> lines.add(SheetLine.Sentence.of(TermText.of("Adds to " + first + ":"))));
        offer.words().stats().forEach(row -> lines.add(Row.plain(Glyph.DOT, row.label(), row.value())));
        offer.words().sentences().forEach(text -> lines.add(SheetLine.Sentence.of(text.asSentence())));
        return new InfoSheet(lines);
    }

    private static SheetLine gate(boolean met, TermText progress) {
        return met ? SheetLine.Sentence.marked(Glyph.CHECK, Palette.UPGRADE_GATE_MET, progress)
                : SheetLine.Sentence.marked(Glyph.CROSS, Palette.UPGRADE_GATE_UNMET, progress);
    }

    /**
     * Its key and name, then after a tab its price, or why it can't be bought yet: the button
     * paints the two as a row.
     */
    static String buttonText(UpgradeOffer offer) {
        String state;
        if (!offer.gateMet()) {
            state = offer.progress().plain();
        } else if (!offer.affordable()) {
            state = "need $" + offer.node().price();
        } else {
            state = "$" + offer.node().price();
        }
        return offer.number() + "  " + offer.node().displayName() + "\t" + state;
    }

    /** "Head" while the slot offers a node or holds one, else why it is closed: "Head: needs Attune". */
    static String slotHeader(UpgradeSlot slot, UpgradeState owned, boolean offered) {
        String name = SheetNumbers.titleCase(slot);
        if (offered || owned.countIn(slot) > 0) {
            return name;
        }
        return switch (slot) {
            case BASE -> name;
            case HEAD -> owned.owns(StandardBaseSlot.ATTUNE_ID) ? name : name + ": needs Attune";
            case SPECIAL -> owned.owns(StandardBaseSlot.AWAKEN_ID) ? name + ": choose a chain first"
                    : name + ": needs Awaken";
        };
    }

    /**
     * Above the XP bar: "XP 120 / 150 to Weak Spot" toward the next node waiting on XP, or the rank
     * once none is: "XP 340 · Hero".
     */
    static String xpLabel(int xp, Optional<UpgradeNode> nextXpGate) {
        return nextXpGate.map(node -> "XP " + xp + " / " + node.xp() + " to " + node.displayName())
                .orElse("XP " + xp + " · " + SheetNumbers.titleCase(TowerRank.of(xp)));
    }

    /** The offered node with the least XP still ahead of {@code xp}: the next one XP opens. */
    static Optional<UpgradeNode> nextXpGate(List<UpgradeNode> offered, int xp) {
        return offered.stream().filter(node -> node.xp() > xp).min(Comparator.comparingInt(UpgradeNode::xp));
    }

    /** Beside a slot's header while an exclusive choice of {@code members} nodes is on offer there. */
    static String choiceLabel(int members) {
        return "1 of " + members;
    }
}
