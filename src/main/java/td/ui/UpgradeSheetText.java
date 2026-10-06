package td.ui;

import td.tower.TowerRank;
import td.tower.upgrade.StandardBaseSlot;
import td.tower.upgrade.UpgradeCondition;
import td.tower.upgrade.UpgradeDecision;
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
     * The node's name and price, a ✔/✘ row for its XP and one for its gate condition (each only
     * when it has one), then its bonuses.
     */
    static InfoSheet hover(UpgradeOffer offer) {
        UpgradeNode node = offer.node();
        List<SheetLine> lines = new ArrayList<>();
        lines.add(new SheetLine.Title(Glyph.PIP, TowerSpriteFrameBuilder.slotPaletteFor(node.slot()), node.displayName(),
                "$" + node.price()));
        if (node.xp() > 0) {
            lines.add(gateRow(offer.xpMet(), offer.xpProgress()));
        }
        if (!node.gate().equals(UpgradeCondition.always())) {
            lines.add(gateRow(offer.conditionMet(), offer.conditionProgress()));
        }
        // The rivals get rows of their own: "Locks out" and a long name don't fit one line together.
        if (offer.inChoice() && offer.lastPick()) {
            lines.add(Row.plain(Glyph.LOCK, "Locks out", ""));
            offer.rivals().forEach(rival -> lines.add(Row.plain(Glyph.DOT, rival.displayName(), "")));
        } else if (offer.inChoice()) {
            lines.add(Row.plain(Glyph.LOCK, "Next pick", "Transcendent"));
        }
        lines.add(new SheetLine.Gap());
        node.bonuses().forEach(bonus -> lines.add(Row.plain(Glyph.DOT, bonus.label(), bonus.value())));
        return new InfoSheet(lines);
    }

    private static Row gateRow(boolean met, String progress) {
        return met
                ? Row.toned(Glyph.CHECK, Palette.UPGRADE_GATE_MET, progress, "")
                : Row.toned(Glyph.CROSS, Palette.UPGRADE_GATE_UNMET, progress, "");
    }

    /**
     * Its key and name, then after a tab its price, or why it can't be bought yet: the button
     * paints the two as a row.
     */
    static String buttonText(UpgradeOffer offer) {
        String state;
        if (!offer.gateMet()) {
            state = offer.progress();
        } else if (!offer.affordable()) {
            state = "need $" + offer.node().price();
        } else {
            state = "$" + offer.node().price();
        }
        return offer.number() + "  " + offer.node().displayName() + "\t" + state;
    }

    /**
     * "Base: Range" for the node last bought in the slot, "Base" while one is offered, or why the
     * slot is closed: "Head: needs Attune", "Special: needs Awaken", or, once Awaken is owned but
     * the specials belong to a chain, "Special: choose a chain first".
     */
    static String slotHeader(UpgradeSlot slot, UpgradeState owned, boolean offered) {
        String name = SheetNumbers.titleCase(slot);
        Optional<UpgradeNode> tip = owned.tip(slot);
        if (tip.isPresent()) {
            return name + ": " + tip.get().displayName();
        }
        if (offered) {
            return name;
        }
        return name + ": " + switch (slot) {
            case BASE -> "locked";
            case HEAD -> owned.owns(StandardBaseSlot.ATTUNE_ID) ? "locked" : "needs Attune";
            case SPECIAL -> owned.owns(StandardBaseSlot.AWAKEN_ID) ? "choose a chain first" : "needs Awaken";
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

    /**
     * One row per node the slot's choices locked out, beside the lock glyph: "chosen over Long
     * Reach". The slot header names what was chosen. A member passed over twice (by both specials)
     * gets one row.
     */
    static List<String> decisionRows(List<UpgradeDecision> decisionsInSlot) {
        return decisionsInSlot.stream()
                .flatMap(decision -> decision.passedOver().stream())
                .map(node -> "chosen over " + node.displayName())
                .distinct()
                .toList();
    }
}
