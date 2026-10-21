package td.ui;

import td.damage.DamageUnits;
import td.term.Side;
import td.term.TermText;
import td.tower.AppliedEffect;
import td.tower.NodeProgress;
import td.tower.NodeWords;
import td.tower.TowerBuffState;
import td.tower.TowerInspection;
import td.tower.TowerRank;
import td.tower.TowerStatLine;
import td.tower.TowerStatRow;
import td.tower.upgrade.UpgradeLine;
import td.tower.upgrade.UpgradeNode;
import td.ui.render.IconFace;
import td.ui.render.InfoSheet;
import td.ui.render.Palette;
import td.ui.render.SheetIcon;
import td.ui.render.SheetLine;
import td.ui.render.SheetLine.Glyph;
import td.ui.render.SheetLine.Row;
import td.ui.render.SheetLine.Trend;
import td.ui.render.TooltipKey;
import td.util.TickRate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Turns a {@link TowerInspection} into the tower pane: stat rows, then icon rows (Perks, Buffs, Debuffs, Applies),
 * then its record. The shop shows the tower as authored; the status shows it as it is now, a changed stat as
 * base → current. Pure: no Swing, no live tower.
 */
final class TowerSheetText {

    private static final int TIME_LABEL_BELOW_TICKS = Math.round(10 * TickRate.TICKS_PER_SECOND);

    private TowerSheetText() {
    }

    /** The toolbar's sheet: price, authored stats, whom it targets, what it applies, then what no row says. */
    static InfoSheet shop(TowerInspection tower) {
        List<SheetLine> lines = new ArrayList<>();
        lines.add(title(tower, "$" + tower.price()));
        lines.add(new SheetLine.Gap());
        tower.stats().forEach(stat -> lines.add(statRow(stat.stat(), TowerStatLine.fixed(stat.stat(), stat.base()))));
        targets(tower).ifPresent(lines::add);
        iconRow("Applies", appliesIcons(tower)).ifPresent(lines::add);
        if (!tower.description().isEmpty()) {
            lines.add(new SheetLine.Gap());
            lines.add(new SheetLine.Prose(TermText.of(tower.description())));
        }
        return new InfoSheet(lines);
    }

    /** A placed tower: current stats, whom it targets, its perks, buffs, debuffs and applies, then its record. */
    static InfoSheet status(TowerInspection tower) {
        List<SheetLine> lines = new ArrayList<>();
        lines.add(title(tower, ""));
        lines.add(new SheetLine.Gap());
        tower.stats().forEach(stat -> lines.add(statRow(stat.stat(), stat)));
        targets(tower).ifPresent(lines::add);
        iconRow("Perks", perkIcons(tower)).ifPresent(lines::add);
        iconRow("Buffs", conditionIcons(tower, Side.BUFF)).ifPresent(lines::add);
        iconRow("Debuffs", conditionIcons(tower, Side.DEBUFF)).ifPresent(lines::add);
        iconRow("Applies", appliesIcons(tower)).ifPresent(lines::add);
        lines.add(Row.toned(Glyph.FILLED_DIAMOND, Palette.TOWER_RANK, "XP",
                tower.xp() + " · " + SheetNumbers.titleCase(TowerRank.of(tower.xp()))));
        lines.add(Row.plain(Glyph.SKULL, "Kills",
                tower.kills() + " · " + Math.round(DamageUnits.inPoints(tower.damageDealt())) + " dmg"));
        return new InfoSheet(lines);
    }

    private static SheetLine.Title title(TowerInspection tower, String value) {
        return new SheetLine.Title(Glyph.TOWER_BODY, TowerSpriteFrameBuilder.bodyPaletteFor(tower.type()),
                tower.name() + " tower", value);
    }

    private static Optional<SheetLine> targets(TowerInspection tower) {
        return tower.targeting().isEmpty() ? Optional.empty()
                : Optional.of(Row.plain(Glyph.DOT, "Targets", tower.targeting()));
    }

    private static Optional<SheetLine> iconRow(String label, List<SheetIcon> icons) {
        return icons.isEmpty() ? Optional.empty() : Optional.of(new SheetLine.IconRow(label, icons));
    }

    private static List<SheetIcon> perkIcons(TowerInspection tower) {
        return tower.lines().stream().map(line -> perkIcon(tower, line)).toList();
    }

    private static SheetIcon perkIcon(TowerInspection tower, UpgradeLine line) {
        IconFace face = TermLooks.faceOfLine(tower.type(), line.artNode(), line.level());
        IconFace shown = progressOf(tower, line).map(progress -> face.withCorner(progress.count() + "/" + progress.of()))
                .orElse(face);
        List<SheetLine> rows = new ArrayList<>();
        rows.add(new SheetLine.IconTitle(face, line.tip().displayName(), face.tone(), ""));
        for (UpgradeNode node : line.owned()) {
            Palette slot = TowerSpriteFrameBuilder.slotPaletteFor(node.slot());
            NodeWords words = tower.words().get(node.id());
            if (line.owned().size() > 1 && words != null) {
                rows.add(Row.toned(Glyph.PIP, slot, words.name(), ""));
            }
            if (words == null) {
                continue;
            }
            words.stats().forEach(stat -> rows.add(Row.plain(Glyph.DOT, stat.label(), stat.value())));
            words.sentences().forEach(text -> rows.add(SheetLine.Sentence.of(text.asSentence())));
            words.chosenOver().forEach(name -> rows.add(SheetLine.Sentence.marked(Glyph.LOCK, slot,
                    TermText.of("chosen over " + name))));
        }
        return new SheetIcon(shown, TooltipKey.of("perk", line.first().id()), new InfoSheet(rows));
    }

    private static Optional<NodeProgress> progressOf(TowerInspection tower, UpgradeLine line) {
        NodeProgress found = null;
        for (NodeProgress progress : tower.progress()) {
            if (line.owned().stream().anyMatch(node -> node.id().equals(progress.nodeId()))) {
                found = progress;
            }
        }
        return Optional.ofNullable(found);
    }

    private static List<SheetIcon> conditionIcons(TowerInspection tower, Side side) {
        return tower.conditions().stream().filter(state -> state.side() == side)
                .map(TowerSheetText::conditionIcon).toList();
    }

    private static SheetIcon conditionIcon(TowerBuffState state) {
        IconFace face = TermLooks.faceOf(state.term());
        if (state.stacks() > 0) {
            face = face.withCorner(Integer.toString(state.stacks()));
        }
        List<SheetLine> rows = new ArrayList<>();
        if (state.remainingTicks().isPresent()) {
            int remaining = state.remainingTicks().getAsInt();
            if (state.clockTicks() > 0) {
                face = face.withElapsed(1f - (float) remaining / state.clockTicks());
            }
            if (remaining < TIME_LABEL_BELOW_TICKS) {
                face = face.withBelow((int) Math.ceil(remaining / (double) TickRate.TICKS_PER_SECOND) + "s");
            }
            rows.add(Row.plain(Glyph.DOT, "Time left", SheetNumbers.decimal(remaining / (float) TickRate.TICKS_PER_SECOND)
                    + " s"));
        }
        if (state.stacks() > 0) {
            rows.add(Row.plain(Glyph.DOT, "Stacks", Integer.toString(state.stacks())));
        }
        return new SheetIcon(face, TooltipKey.ofTerm("condition", state.term()),
                TermSheets.of(state.term(), state.detail(), rows));
    }

    private static List<SheetIcon> appliesIcons(TowerInspection tower) {
        return tower.applies().stream().map(TowerSheetText::appliesIcon).toList();
    }

    private static SheetIcon appliesIcon(AppliedEffect applied) {
        List<SheetLine> rows = new ArrayList<>();
        rows.add(new SheetLine.Divider());
        applied.sources().forEach(source -> rows.add(SheetLine.Sentence.of(
                TermText.of(source.name() + ": ").plus(source.how()).asSentence())));
        return new SheetIcon(TermLooks.faceOf(applied.term()), TooltipKey.ofTerm("applies", applied.term()),
                TermSheets.of(applied.term(), applied.term().describe(), rows));
    }

    private static Row statRow(TowerStatRow stat, TowerStatLine line) {
        NumberText text = stat == TowerStatRow.CRIT_CHANCE ? SheetNumbers::percent : SheetNumbers::decimal;
        String base = text.of(line.base());
        String current = text.of(line.current());
        String value = (base.equals(current) ? current : base + " → " + current) + switch (stat) {
            case FIRE_RATE -> "/s";
            case ROTATION -> " s/turn";
            case PROJECTILE_SPEED -> " cells/s";
            case PROJECTILE_SIZE -> "x";
            case RANGE, PHYSICAL_DAMAGE, MAGIC_DAMAGE, CRIT_CHANCE, SPLASH_RADIUS -> "";
        };
        Row row = switch (stat) {
            case RANGE -> Row.plain(Glyph.RING, "Range", value);
            case PHYSICAL_DAMAGE -> Row.toned(Glyph.DOT, Palette.DAMAGE_PHYSICAL, "Physical damage", value);
            case MAGIC_DAMAGE -> Row.toned(Glyph.DOT, Palette.DAMAGE_MAGIC, "Magic damage", value);
            case FIRE_RATE -> Row.plain(Glyph.CHEVRON, "Fire rate", value);
            case ROTATION -> Row.plain(Glyph.CHEVRON, "Rotation", value);
            case CRIT_CHANCE -> Row.toned(Glyph.SPARK, Palette.CRIT_SPARK, "Crit chance", value);
            case SPLASH_RADIUS -> Row.plain(Glyph.RING, "Splash radius", value);
            case PROJECTILE_SPEED -> Row.plain(Glyph.CHEVRON, "Projectile speed", value);
            case PROJECTILE_SIZE -> Row.plain(Glyph.DOT, "Projectile size", value);
        };
        if (base.equals(current)) {
            return row;
        }
        // A slower rotation is the one stat that is worse higher.
        boolean better = (line.current() > line.base()) == (stat != TowerStatRow.ROTATION);
        return row.withTrend(better ? Trend.BETTER : Trend.WORSE);
    }

    private interface NumberText {
        String of(float value);
    }
}
