package td.ui;

import td.tower.upgrade.UpgradeCondition;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeSlot;
import td.ui.render.InfoSheet;
import td.ui.render.Palette;
import td.ui.render.SheetLine;
import td.ui.render.SheetLine.Glyph;
import td.ui.render.SheetLine.Row;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** The words for an upgrade node: its hover sheet, its button face and its slot's header. Pure. */
final class UpgradeSheetText {

    private UpgradeSheetText() {
    }

    /** The node's name and price, its gate as a ✔/✘ row unless money alone buys it, then its bonuses. */
    static InfoSheet hover(UpgradeOffer offer) {
        UpgradeNode node = offer.node();
        List<SheetLine> lines = new ArrayList<>();
        lines.add(new SheetLine.Title(Glyph.PIP, TowerSpriteFrameBuilder.slotPaletteFor(node.slot()), node.displayName(),
                "$" + node.price()));
        if (!node.gate().equals(UpgradeCondition.always())) {
            lines.add(offer.gateMet()
                    ? Row.toned(Glyph.CHECK, Palette.UPGRADE_GATE_MET, offer.progress(), "")
                    : Row.toned(Glyph.CROSS, Palette.UPGRADE_GATE_UNMET, offer.progress(), ""));
        }
        lines.add(new SheetLine.Gap());
        node.bonuses().forEach(bonus -> lines.add(Row.plain(Glyph.DOT, bonus.label(), bonus.value())));
        return new InfoSheet(lines);
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

    /** "Base: Range" once a node is owned, "Base" while one is offered, else "Special: locked". */
    static String slotHeader(UpgradeSlot slot, Optional<UpgradeNode> owned, boolean offered) {
        String name = SheetNumbers.titleCase(slot);
        return owned.map(node -> name + ": " + node.displayName()).orElse(offered ? name : name + ": locked");
    }
}
