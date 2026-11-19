package td.ui;

import td.economy.Refund;
import td.economy.SellRefund;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradePath;
import td.tower.upgrade.UpgradeTier;
import td.tower.upgrade.UpgradeTree;
import td.ui.render.InfoSheet;
import td.ui.render.Palette;
import td.ui.render.SheetLine;
import td.ui.render.SheetLine.Glyph;
import td.ui.render.SheetLine.Row;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** The words for an upgrade node: its hover sheet, the panel's footer and the downgrade confirmation. Pure. */
final class UpgradeSheetText {

    /** The footer while nothing is hovered. */
    static final String IDLE_FOOTER = "Click: buy · Right-click: downgrade";
    /** The footer once the level is over and nothing can be bought or downgraded. */
    static final String ENDED_FOOTER = "Level over";
    /** A tier's floor strip once every single in it is owned. */
    static final String SINGLES_BOUGHT = "Singles bought";
    /** Why a switch or a downgrade waits while a wave is out. */
    static final String BETWEEN_WAVES = "Changes between waves";

    private UpgradeSheetText() {
    }

    /**
     * The node's face, name and price; what its tier still needs; what a tier's unlock opens; the cluster it is one
     * of; then what it adds: its value rows and its sentences.
     */
    static InfoSheet hover(UpgradeOffer offer) {
        return new InfoSheet(lines(offer, true));
    }

    /** {@link #hover(UpgradeOffer)} followed by what a click on it would do: the path it buys, or the downgrade. */
    static InfoSheet hover(UpgradeOffer offer, UpgradeHover hover, UpgradeTree tree) {
        List<SheetLine> lines = new ArrayList<>();
        switch (hover) {
            case UpgradeHover.Buying buying -> {
                // A path names everything it locks out, its own pick's rivals among them.
                lines.addAll(lines(offer, buying.path().steps().isEmpty()));
                lines.addAll(pathLines(buying, tree));
            }
            case UpgradeHover.Switching switching -> {
                lines.addAll(lines(offer, false));
                lines.add(new SheetLine.Divider());
                lines.addAll(switchLines(switching));
                lines.add(switching.allowed()
                        ? SheetLine.Sentence.marked(Glyph.CHECK, Palette.UPGRADE_GATE_MET, TermText.of("Click to switch"))
                        : SheetLine.Sentence.marked(Glyph.CROSS, Palette.UPGRADE_GATE_UNMET, TermText.of(BETWEEN_WAVES)));
            }
            case UpgradeHover.Downgrading downgrading -> {
                lines.addAll(lines(offer, false));
                lines.add(new SheetLine.Divider());
                lines.addAll(downgradeLines(downgrading, tree));
                lines.add(SheetLine.Sentence.marked(Glyph.CROSS, Palette.UPGRADE_GATE_UNMET,
                        TermText.of(downgrading.allowed() ? "Right-click to downgrade" : BETWEEN_WAVES)));
            }
        }
        return new InfoSheet(lines);
    }

    private static List<SheetLine> lines(UpgradeOffer offer, boolean rivals) {
        UpgradeNode node = offer.node();
        List<SheetLine> lines = new ArrayList<>();
        lines.add(new SheetLine.IconTitle(offer.face(), node.displayName(), offer.face().tone(), TermNumbers.cash(node.price())));
        if (!offer.tierOpen()) {
            lines.add(gate(false, TermText.of(offer.tierNeed())));
        }
        if (offer.unlock()) {
            lines.add(SheetLine.Sentence.of(TermText.of("Opens " + UpgradeTier.nameOf(node.tier())
                    + ": every upgrade in it then costs only credits.")));
        }
        // The rivals get rows of their own: a cluster's name and a long member's don't fit one line together.
        if (offer.inCluster() && rivals) {
            lines.add(Row.plain(Glyph.LOCK, "One of " + offer.cluster().orElse(""), ""));
            offer.rivals().forEach(rival -> lines.add(Row.plain(Glyph.DOT, rival.displayName(), "")));
        }
        lines.add(new SheetLine.Gap());
        offer.addsTo().ifPresent(first -> lines.add(SheetLine.Sentence.of(TermText.of("Adds to " + first + ":"))));
        offer.words().stats().forEach(bonus -> lines.add(bonus.preview()
                .map(preview -> StatRowText.change(bonus.stat(), bonus.change(), preview, TowerSheetText.WORDING))
                .orElseGet(() -> TowerSheetText.bonusRow(bonus))));
        offer.words().sentences().forEach(text -> lines.add(SheetLine.Sentence.of(text.asSentence())));
        List<SheetLine> definitions = definitions(offer.words().sentences());
        if (!definitions.isEmpty()) {
            lines.add(new SheetLine.Divider());
            lines.addAll(definitions);
        }
        return lines;
    }

    /**
     * What each term the sentences mention means there, in the order they first name it: a node's sentence often
     * only points at a term, and the term is what the player is buying. A mention repeated with the same meaning is
     * defined once; the terms a definition mentions are left to their own hover.
     */
    private static List<SheetLine> definitions(List<TermText> sentences) {
        return sentences.stream().flatMap(text -> text.spans().stream())
                .filter(TermText.Mention.class::isInstance).map(TermText.Mention.class::cast)
                .distinct()
                .<SheetLine>map(mention -> new SheetLine.Prose(new TermText(List.of(mention)).plus(" — ")
                        .plus(mention.meaning().asSentence())))
                .toList();
    }

    /** What the downgrade confirmation lists: the nodes going and their prices, the refund, what reopens. */
    static InfoSheet downgradeConfirmation(UpgradeHover.Downgrading downgrading, UpgradeTree tree) {
        List<SheetLine> lines = new ArrayList<>();
        lines.add(new SheetLine.Title(Glyph.CROSS, Palette.UPGRADE_GATE_UNMET, "Downgrade", ""));
        lines.addAll(downgradeLines(downgrading, tree));
        return new InfoSheet(lines);
    }

    /** What the switch confirmation lists: the member it replaces, what is sold back and the refund. */
    static InfoSheet switchConfirmation(UpgradeHover.Switching switching) {
        List<SheetLine> lines = new ArrayList<>();
        lines.add(new SheetLine.Title(Glyph.CHEVRON, Palette.UPGRADE_GATE_MET,
                "Switch to " + switching.change().to().displayName(), ""));
        lines.addAll(switchLines(switching));
        return new InfoSheet(lines);
    }

    /** "Switch ( +$12 )", the confirmation's button. */
    static String switchButton(UpgradeHover.Switching switching) {
        return "Switch ( +$" + switching.refund().total() + " )";
    }

    /** "Downgrade ( +$29 )", the confirmation's button. */
    static String downgradeButton(UpgradeHover.Downgrading downgrading) {
        return "Downgrade ( +$" + downgrading.refund().total() + " )";
    }

    /**
     * The panel's footer for what is hovered: "2 steps · $48" for a path, with what stops it, or "Right-click:
     * downgrade 3 · +$87" on an owned node; {@link #IDLE_FOOTER} for none.
     */
    static String footer(Optional<UpgradeHover> hover) {
        if (hover.isEmpty()) {
            return IDLE_FOOTER;
        }
        return switch (hover.get()) {
            case UpgradeHover.Buying buying -> {
                UpgradePath path = buying.path();
                String steps = path.steps().size() == 1 ? "1 step" : path.steps().size() + " steps";
                String text = steps + " · $" + path.total();
                if (path.block().isPresent()) {
                    yield text + " · blocked: " + reasonWord(path.block().get().reason());
                }
                yield buying.affordable() ? text : text + " · short $" + buying.shortBy();
            }
            case UpgradeHover.Switching switching when !switching.allowed() -> BETWEEN_WAVES;
            case UpgradeHover.Switching switching -> switching.change().sold().isEmpty() ? "Click: switch · free"
                    : "Click: switch · sells " + switching.change().sold().size() + " · +$" + switching.refund().total();
            case UpgradeHover.Downgrading downgrading when !downgrading.allowed() -> BETWEEN_WAVES;
            case UpgradeHover.Downgrading downgrading -> "Right-click: downgrade " + downgrading.removal().removed().size()
                    + " · +$" + downgrading.refund().total() + (downgrading.refund().isFull() ? " · full" : "");
        };
    }

    /**
     * Whether the footer for {@code hover} warns: a path that a click won't buy, a change that waits for the wave's
     * end, or a downgrade that loses money.
     */
    static boolean footerWarns(Optional<UpgradeHover> hover) {
        return hover.map(h -> switch (h) {
            case UpgradeHover.Buying buying -> !buying.buys();
            case UpgradeHover.Switching switching -> !switching.allowed();
            case UpgradeHover.Downgrading downgrading -> !downgrading.allowed() || !downgrading.refund().isFull();
        }).orElse(false);
    }

    /** What a switch sells back, and the refund. */
    private static List<SheetLine> switchLines(UpgradeHover.Switching switching) {
        List<SheetLine> lines = new ArrayList<>();
        lines.add(Row.plain(Glyph.CHEVRON, "Replaces", switching.change().from().displayName()));
        if (switching.change().sold().isEmpty()) {
            return lines;
        }
        lines.add(Row.plain(Glyph.CROSS, "Sells back", ""));
        switching.change().sold().forEach(node -> lines.add(Row.plain(Glyph.DOT, node.displayName(),
                TermNumbers.cash(node.price()))));
        lines.addAll(refundLines(switching.refund()));
        return lines;
    }

    private static List<SheetLine> pathLines(UpgradeHover.Buying buying, UpgradeTree tree) {
        UpgradePath path = buying.path();
        if (path.steps().isEmpty()) {
            return path.block().filter(block -> block.reason() == UpgradePath.Reason.LOCKED_OUT)
                    .map(block -> List.<SheetLine>of(SheetLine.Sentence.marked(Glyph.CROSS, Palette.UPGRADE_GATE_UNMET,
                            TermText.of("Locked out by an earlier pick"))))
                    .orElse(List.of());
        }
        List<SheetLine> lines = new ArrayList<>();
        lines.add(new SheetLine.Divider());
        Optional<UpgradeNode> blockedAt = path.block().map(UpgradePath.Block::at);
        if (path.steps().size() > 1) {
            lines.add(Row.plain(Glyph.CHEVRON, "Path · " + path.steps().size() + " steps", ""));
            for (UpgradeNode step : path.steps()) {
                lines.add(blockedAt.filter(step::equals).isPresent()
                        ? Row.toned(Glyph.CROSS, Palette.UPGRADE_GATE_UNMET, step.displayName(), TermNumbers.cash(step.price()))
                        : Row.plain(Glyph.DOT, step.displayName(), TermNumbers.cash(step.price())));
            }
            lines.add(Row.plain(Glyph.CHEVRON, "Total", TermNumbers.cash(path.total())));
        }
        if (path.block().isPresent()) {
            UpgradePath.Block block = path.block().get();
            lines.add(SheetLine.Sentence.marked(Glyph.CROSS, Palette.UPGRADE_GATE_UNMET,
                    TermText.of("Blocked: " + block.at().displayName() + " " + reasonPhrase(block.reason()))));
        } else if (!buying.affordable()) {
            lines.add(SheetLine.Sentence.marked(Glyph.CROSS, Palette.UPGRADE_GATE_UNMET,
                    TermText.of("Short $" + buying.shortBy())));
        } else {
            lines.add(SheetLine.Sentence.marked(Glyph.CHECK, Palette.UPGRADE_GATE_MET,
                    TermText.of(path.steps().size() == 1 ? "Click to buy" : "Click to buy all " + path.steps().size())));
        }
        List<String> lockedLines = lineNames(path.locksOut(), tree);
        if (!lockedLines.isEmpty()) {
            lines.add(Row.toned(Glyph.LOCK, Palette.UPGRADE_GATE_UNMET, "Locks out", ""));
            lockedLines.forEach(name -> lines.add(Row.plain(Glyph.DOT, name, "")));
        }
        return lines;
    }

    private static List<SheetLine> downgradeLines(UpgradeHover.Downgrading downgrading, UpgradeTree tree) {
        List<SheetLine> lines = new ArrayList<>();
        downgrading.removal().removed().forEach(node -> lines.add(Row.plain(Glyph.DOT, node.displayName(),
                TermNumbers.cash(node.price()))));
        lines.addAll(refundLines(downgrading.refund()));
        List<String> reopened = lineNames(downgrading.reopens(), tree);
        if (!reopened.isEmpty()) {
            lines.add(Row.plain(Glyph.LOCK, "Reopens", ""));
            reopened.forEach(name -> lines.add(Row.plain(Glyph.DOT, name, "")));
        }
        return lines;
    }

    private static List<SheetLine> refundLines(Refund refund) {
        List<SheetLine> lines = new ArrayList<>();
        if (refund.inFull() > 0) {
            lines.add(Row.toned(Glyph.CASH, Palette.TERM_STATUS_CASH, "Refund 100%", "+$" + refund.inFull()));
        }
        if (refund.inFull() == 0 || refund.shared() > 0) {
            lines.add(Row.toned(Glyph.CHEVRON, Palette.UPGRADE_GATE_MET, "Refund " + Math.round(SellRefund.SHARE * 100) + "%",
                    "+$" + refund.shared()));
        }
        return lines;
    }

    /** {@code nodes} by line: a line's name once ("Marksman's Eye line"), a lone node by its own name. */
    private static List<String> lineNames(List<UpgradeNode> nodes, UpgradeTree tree) {
        Map<UpgradeNode, List<UpgradeNode>> byLine = new LinkedHashMap<>();
        nodes.forEach(node -> byLine.computeIfAbsent(tree.firstOf(node), first -> new ArrayList<>()).add(node));
        return byLine.entrySet().stream()
                .map(line -> line.getValue().size() > 1 ? line.getKey().displayName() + " line"
                        : line.getValue().getFirst().displayName())
                .toList();
    }

    private static String reasonPhrase(UpgradePath.Reason reason) {
        return switch (reason) {
            case OWNED -> "is owned";
            case LOCKED_OUT -> "is locked out";
            case CHOOSE_FIRST -> "is a choice to make first";
            case UNREACHABLE -> "can't be reached";
            case XP -> "needs more XP";
        };
    }

    private static String reasonWord(UpgradePath.Reason reason) {
        return switch (reason) {
            case OWNED -> "owned";
            case LOCKED_OUT -> "locked out";
            case CHOOSE_FIRST -> "choose first";
            case UNREACHABLE -> "unreachable";
            case XP -> "XP";
        };
    }

    private static SheetLine gate(boolean met, TermText progress) {
        return met ? SheetLine.Sentence.marked(Glyph.CHECK, Palette.UPGRADE_GATE_MET, progress)
                : SheetLine.Sentence.marked(Glyph.CROSS, Palette.UPGRADE_GATE_UNMET, progress);
    }

    /** The tower's XP on the panel's title line: "XP 70". */
    static String titleXp(int xp) {
        return "XP " + xp;
    }

    /** A locked tier's gate: "XP 70 / 120", the XP held at the gate's once it is met. */
    static String gateLabel(int xp, int needed) {
        return "XP " + Math.min(xp, needed) + " / " + needed;
    }

    /** The button that buys a tier's singles on offer, for {@code total} credits together. */
    static String singlesButton(int total) {
        return "Buy singles · " + TermNumbers.cash(total);
    }

    /** The footer while the singles' button is hovered: "3 singles · $24". */
    static String singlesFooter(int count, int total) {
        return count + (count == 1 ? " single · " : " singles · ") + TermNumbers.cash(total);
    }

    /** The footer over a slot that stands empty until one of {@code members} is picked: "Pick A or B first". */
    static String pickFirst(List<String> members) {
        return "Pick " + String.join(" or ", members) + " first";
    }
}
