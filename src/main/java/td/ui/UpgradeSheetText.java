package td.ui;

import td.term.TermText;
import td.tower.TowerRank;
import td.tower.upgrade.UpgradeCondition;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradePath;
import td.tower.upgrade.UpgradeTree;
import td.ui.render.InfoSheet;
import td.ui.render.Palette;
import td.ui.render.SheetLine;
import td.ui.render.SheetLine.Glyph;
import td.ui.render.SheetLine.Row;

import java.util.ArrayList;
import java.util.Comparator;
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

    private UpgradeSheetText() {
    }

    /**
     * The node's face, name and price; a ✔/✘ line for its XP and one for its gate; the Core step it waits for;
     * what it locks out or leaves for a later pick; then what it adds: its value rows and its sentences.
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
            case UpgradeHover.Downgrading downgrading -> {
                lines.addAll(lines(offer, false));
                lines.add(new SheetLine.Divider());
                lines.addAll(downgradeLines(downgrading, tree));
                lines.add(SheetLine.Sentence.marked(Glyph.CROSS, Palette.UPGRADE_GATE_UNMET,
                        TermText.of("Right-click to downgrade")));
            }
        }
        return new InfoSheet(lines);
    }

    private static List<SheetLine> lines(UpgradeOffer offer, boolean rivals) {
        UpgradeNode node = offer.node();
        List<SheetLine> lines = new ArrayList<>();
        lines.add(new SheetLine.IconTitle(offer.face(), node.displayName(), offer.face().tone(), "$" + node.price()));
        if (node.xp() > 0) {
            lines.add(gate(offer.xpMet(), TermText.of(offer.xpProgress())));
        }
        if (!node.gate().equals(UpgradeCondition.always())) {
            lines.add(gate(offer.conditionMet(), offer.conditionProgress()));
        }
        offer.sealedBy().ifPresent(core -> lines.add(Row.toned(Glyph.LOCK, Palette.TOWER_UPGRADE_CORE, "Opens with",
                core.displayName())));
        // The rivals get rows of their own: "Locks out" and a long name don't fit one line together.
        if (offer.inChoice() && offer.lastPick()) {
            if (rivals) {
                lines.add(Row.plain(Glyph.LOCK, "Locks out", ""));
                offer.rivals().forEach(rival -> lines.add(Row.plain(Glyph.DOT, rival.displayName(), "")));
            }
        } else if (offer.inChoice() && offer.nextPickWaits()) {
            lines.add(Row.plain(Glyph.LOCK, "Next pick", "Transcendent"));
        }
        lines.add(new SheetLine.Gap());
        offer.addsTo().ifPresent(first -> lines.add(SheetLine.Sentence.of(TermText.of("Adds to " + first + ":"))));
        offer.words().stats().forEach(row -> lines.add(Row.plain(Glyph.DOT, row.label(), row.value())));
        offer.words().sentences().forEach(text -> lines.add(SheetLine.Sentence.of(text.asSentence())));
        return lines;
    }

    /** What the downgrade confirmation lists: the nodes going and their prices, the refund, what reopens. */
    static InfoSheet downgradeConfirmation(UpgradeHover.Downgrading downgrading, UpgradeTree tree) {
        List<SheetLine> lines = new ArrayList<>();
        lines.add(new SheetLine.Title(Glyph.CROSS, Palette.UPGRADE_GATE_UNMET, "Downgrade", ""));
        lines.addAll(downgradeLines(downgrading, tree));
        return new InfoSheet(lines);
    }

    /** "Downgrade ( +$29 )", the confirmation's button. */
    static String downgradeButton(UpgradeHover.Downgrading downgrading) {
        return "Downgrade ( +$" + downgrading.refund() + " )";
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
                yield buying.affordable() ? text : text + " · short $" + (path.total() - buying.credits());
            }
            case UpgradeHover.Downgrading downgrading -> "Right-click: downgrade " + downgrading.removal().removed().size()
                    + " · +$" + downgrading.refund();
        };
    }

    /** Whether the footer for {@code hover} warns: a path that a click won't buy, or a downgrade. */
    static boolean footerWarns(Optional<UpgradeHover> hover) {
        return hover.map(h -> switch (h) {
            case UpgradeHover.Buying buying -> !buying.buys();
            case UpgradeHover.Downgrading downgrading -> true;
        }).orElse(false);
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
                        ? Row.toned(Glyph.CROSS, Palette.UPGRADE_GATE_UNMET, step.displayName(), "$" + step.price())
                        : Row.plain(Glyph.DOT, step.displayName(), "$" + step.price()));
            }
            lines.add(Row.plain(Glyph.CHEVRON, "Total", "$" + path.total()));
        }
        if (path.block().isPresent()) {
            UpgradePath.Block block = path.block().get();
            lines.add(SheetLine.Sentence.marked(Glyph.CROSS, Palette.UPGRADE_GATE_UNMET,
                    TermText.of("Blocked: " + block.at().displayName() + " " + reasonPhrase(block.reason()))));
        } else if (!buying.affordable()) {
            lines.add(SheetLine.Sentence.marked(Glyph.CROSS, Palette.UPGRADE_GATE_UNMET,
                    TermText.of("Short $" + (path.total() - buying.credits()))));
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
                "$" + node.price())));
        lines.add(Row.toned(Glyph.CHEVRON, Palette.UPGRADE_GATE_MET, "Refund 75%", "+$" + downgrading.refund()));
        List<String> reopened = lineNames(downgrading.reopens(), tree);
        if (!reopened.isEmpty()) {
            lines.add(Row.plain(Glyph.LOCK, "Reopens", ""));
            reopened.forEach(name -> lines.add(Row.plain(Glyph.DOT, name, "")));
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
            case GATE -> "has an unmet gate";
        };
    }

    private static String reasonWord(UpgradePath.Reason reason) {
        return switch (reason) {
            case OWNED -> "owned";
            case LOCKED_OUT -> "locked out";
            case CHOOSE_FIRST -> "choose first";
            case UNREACHABLE -> "unreachable";
            case XP -> "XP";
            case GATE -> "gate";
        };
    }

    private static SheetLine gate(boolean met, TermText progress) {
        return met ? SheetLine.Sentence.marked(Glyph.CHECK, Palette.UPGRADE_GATE_MET, progress)
                : SheetLine.Sentence.marked(Glyph.CROSS, Palette.UPGRADE_GATE_UNMET, progress);
    }

    /**
     * Beside the XP bar: "XP 120 / 150 to Weak Spot" toward the next node waiting on XP, or the rank
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
}
