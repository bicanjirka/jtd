package td.ui;

import td.effect.EffectKind;
import td.tower.BehaviourLine;
import td.tower.TowerInspection;
import td.tower.TowerStat;
import td.tower.TowerStatLine;
import td.tower.upgrade.UpgradeSlot;
import td.ui.render.InfoSheet;
import td.ui.render.Palette;
import td.ui.render.SheetLine;
import td.ui.render.SheetLine.Glyph;
import td.ui.render.SheetLine.Row;
import td.ui.render.SheetLine.Trend;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Turns a {@link TowerInspection} into info-panel rows. Pure: no Swing, no live tower.
 * <p>
 * One row per stat or behaviour, marked with what the board draws for it. The shop shows the
 * tower as authored; the status shows it as it is now, a changed stat as base → current, and
 * leaves out what the Upgrades panel already offers.
 */
final class TowerSheetText {

    private TowerSheetText() {
    }

    /** The toolbar description: price, authored stats and behaviours, then what no row says. */
    static InfoSheet shop(TowerInspection tower) {
        List<SheetLine> lines = new ArrayList<>();
        lines.add(title(tower, "$" + tower.price()));
        lines.add(new SheetLine.Gap());
        tower.stats().forEach(stat -> lines.add(statRow(stat.stat(), TowerStatLine.fixed(stat.stat(), stat.base()))));
        tower.behaviours().forEach(behaviour -> lines.add(behaviourRow(behaviour)));
        if (!tower.description().isEmpty()) {
            lines.add(new SheetLine.Gap());
            lines.add(new SheetLine.Prose(tower.description()));
        }
        return new InfoSheet(lines);
    }

    /**
     * A placed tower: sell value, current stats and behaviours, what changes them, its record, then
     * the upgrade each slot holds.
     */
    static InfoSheet status(TowerInspection tower) {
        List<SheetLine> lines = new ArrayList<>();
        lines.add(title(tower, "sell $" + tower.sellPrice()));
        lines.add(new SheetLine.Gap());
        tower.stats().forEach(stat -> lines.add(statRow(stat.stat(), stat)));
        tower.behaviours().forEach(behaviour -> lines.add(behaviourRow(behaviour)));
        if (!tower.disruption().isNone()) {
            lines.add(Row.effect(Palette.DISRUPTION, "Jammed", ""));
        }
        if (tower.auras() > 0) {
            lines.add(Row.toned(Glyph.RING, Palette.TOWER_AURA_RING, "Aura", tower.auras() > 1 ? "x" + tower.auras() : ""));
        }
        lines.add(Row.plain(Glyph.SKULL, "Kills", tower.kills() + " · " + Math.round(tower.damageDealt() / 100f) + " dmg"));
        List<Row> upgrades = new ArrayList<>();
        for (UpgradeSlot slot : UpgradeSlot.values()) {
            tower.upgrades().tip(slot).ifPresent(node -> upgrades.add(Row.toned(Glyph.PIP,
                    TowerSpriteFrameBuilder.slotPaletteFor(slot), node.displayName(), slot.name().toLowerCase(Locale.ROOT))));
        }
        if (!upgrades.isEmpty()) {
            lines.add(new SheetLine.Gap());
            lines.addAll(upgrades);
        }
        return new InfoSheet(lines);
    }

    private static SheetLine.Title title(TowerInspection tower, String value) {
        return new SheetLine.Title(Glyph.TOWER_BODY, TowerSpriteFrameBuilder.bodyPaletteFor(tower.type()),
                SheetNumbers.titleCase(tower.type()) + " tower", value);
    }

    private static Row statRow(TowerStat stat, TowerStatLine line) {
        NumberText text = stat == TowerStat.CRIT_CHANCE ? SheetNumbers::percent : SheetNumbers::decimal;
        String base = text.of(line.base());
        String current = text.of(line.current());
        String value = (base.equals(current) ? current : base + " → " + current) + switch (stat) {
            case FIRE_RATE -> "/s";
            case ROTATION -> " s/turn";
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
        };
        if (base.equals(current)) {
            return row;
        }
        // A slower rotation is the one stat that is worse higher.
        boolean better = (line.current() > line.base()) == (stat != TowerStat.ROTATION);
        return row.withTrend(better ? Trend.BETTER : Trend.WORSE);
    }

    private static Row behaviourRow(BehaviourLine behaviour) {
        return switch (behaviour.marker()) {
            case TARGETING -> Row.plain(Glyph.DOT, behaviour.label(), behaviour.value());
            case SLOW -> Row.effect(EnemyFrameBuilder.markerPaletteFor(EffectKind.SLOW), behaviour.label(), behaviour.value());
            case BURN -> Row.effect(EnemyFrameBuilder.markerPaletteFor(EffectKind.BURN), behaviour.label(), behaviour.value());
            case FREEZE -> Row.effect(EnemyFrameBuilder.markerPaletteFor(EffectKind.FREEZE), behaviour.label(), behaviour.value());
            case BUFF -> Row.toned(Glyph.RING, Palette.TOWER_AURA_RING, behaviour.label(), behaviour.value());
        };
    }

    private interface NumberText {
        String of(float value);
    }
}
