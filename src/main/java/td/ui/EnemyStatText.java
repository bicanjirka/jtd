package td.ui;

import td.enemy.EnemyInspection;
import td.enemy.HitResolution;
import td.enemy.Rank;
import td.enemy.TraitLine;
import td.stat.EnemyStat;
import td.ui.render.EnemySheet;
import td.ui.render.SheetLine;
import td.ui.render.SheetLine.Glyph;
import td.ui.render.SheetLine.Row;
import td.util.TickRate;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Turns an {@link EnemyInspection} into info-panel rows. Pure: no Swing, no live mob.
 * <p>
 * One row per thing the player can see on the board. A stat at its default is left out. A stat
 * that says what a trait does takes that trait's colour and stands in for it, so no trait is
 * described twice; a trait whose stats show nothing (hurt speed, an adaptive resist not yet
 * resolved) gets its own row.
 */
final class EnemyStatText {

    private EnemyStatText() {
    }

    /** The wave-preview hover: identity, full health, stats and traits, then the description. */
    static EnemySheet preview(EnemyInspection inspection) {
        List<SheetLine> lines = new ArrayList<>();
        lines.add(header(inspection));
        lines.add(new SheetLine.HealthBar(inspection.maxHealth(), inspection.maxHealth(), "$" + inspection.bounty(), false));
        lines.add(new SheetLine.Gap());
        lines.addAll(statAndTraitRows(inspection, false));
        if (!inspection.description().isEmpty()) {
            lines.add(new SheetLine.Gap());
            lines.add(new SheetLine.Prose(inspection.description()));
        }
        return new EnemySheet(lines);
    }

    /** The live inspector: current health and fate, stats, traits, then effects with time left. */
    static EnemySheet live(EnemyInspection inspection) {
        List<SheetLine> lines = new ArrayList<>();
        lines.add(header(inspection));
        lines.add(switch (inspection.fate()) {
            case ALIVE -> new SheetLine.HealthBar(inspection.health(), inspection.maxHealth(), "$" + inspection.bounty(), false);
            case KILLED -> new SheetLine.HealthBar(inspection.health(), inspection.maxHealth(), "Killed", true);
            case LEAKED -> new SheetLine.HealthBar(inspection.health(), inspection.maxHealth(), "Leaked", true);
        });
        lines.add(new SheetLine.Gap());
        lines.addAll(statAndTraitRows(inspection, true));
        inspection.effects().forEach(effect -> lines.add(effectRow(effect)));
        return new EnemySheet(lines);
    }

    private static SheetLine.Header header(EnemyInspection inspection) {
        return new SheetLine.Header(EnemyFrameBuilder.paletteFor(inspection.archetype()),
                EnemyFrameBuilder.badgeFor(inspection.rank()), inspection.name(), titleCase(inspection.rank()));
    }

    /** Speed first, which always shows, then every other stat off its default, then traits no stat covers. */
    private static List<Row> statAndTraitRows(EnemyInspection inspection, boolean live) {
        List<Row> rows = new ArrayList<>();
        StatText speed = statText(EnemyStat.MOVE_SPEED, inspection.stat(EnemyStat.MOVE_SPEED), inspection, live);
        rows.add(Row.plain(Glyph.CHEVRON, speed.label(), speed.value()));
        Set<EnemyStat> shown = EnumSet.noneOf(EnemyStat.class);
        for (EnemyStat stat : EnemyStat.values()) {
            if (stat == EnemyStat.MOVE_SPEED || shown.contains(stat) || !differsFromDefault(inspection, stat)) {
                continue;
            }
            Optional<EnemyStat> pair = pairedWith(stat).filter(other -> inspection.stat(other) == inspection.stat(stat));
            shown.add(stat);
            pair.ifPresent(shown::add);
            StatText text = pair.isPresent() ? pairText(stat, inspection.stat(stat)) : statText(stat, inspection.stat(stat), inspection, live);
            rows.add(toned(inspection, stat, text));
        }
        for (TraitLine trait : inspection.traitLines()) {
            if (shown.stream().noneMatch(trait.marker()::isShownAs)) {
                rows.add(Row.trait(EnemyFrameBuilder.traitMarkerPaletteFor(trait.marker()), trait.label(), trait.value()));
            }
        }
        return rows;
    }

    /** In the colour of the first trait this stat stands in for, else a plain dot. */
    private static Row toned(EnemyInspection inspection, EnemyStat stat, StatText text) {
        return inspection.traitLines().stream()
                .filter(trait -> trait.marker().isShownAs(stat))
                .findFirst()
                .map(trait -> Row.trait(EnemyFrameBuilder.traitMarkerPaletteFor(trait.marker()), text.label(), text.value()))
                .orElseGet(() -> Row.plain(Glyph.DOT, text.label(), text.value()));
    }

    private static boolean differsFromDefault(EnemyInspection inspection, EnemyStat stat) {
        return inspection.stat(stat) != stat.defaultBase();
    }

    /** The stat a row can merge with when both are equal, e.g. armor and magic resist into "all". */
    private static Optional<EnemyStat> pairedWith(EnemyStat stat) {
        return switch (stat) {
            case ARMOR -> Optional.of(EnemyStat.MAGIC_RESIST);
            case PHYSICAL_PLATING -> Optional.of(EnemyStat.MAGIC_PLATING);
            default -> Optional.empty();
        };
    }

    private static StatText pairText(EnemyStat stat, float value) {
        if (stat == EnemyStat.PHYSICAL_PLATING) {
            return new StatText("Plating", platingText(value));
        }
        return new StatText(value >= 0 ? "Resist all" : "Weak to all", mitigationText(value));
    }

    static StatText statText(EnemyStat stat, float value, EnemyInspection inspection, boolean live) {
        return switch (stat) {
            case ARMOR -> new StatText(value >= 0 ? "Resist physical" : "Weak to physical", mitigationText(value));
            case MAGIC_RESIST -> new StatText(value >= 0 ? "Resist magic" : "Weak to magic", mitigationText(value));
            case PHYSICAL_PLATING -> new StatText("Plating, physical", platingText(value));
            case MAGIC_PLATING -> new StatText("Plating, magic", platingText(value));
            case MOVE_SPEED -> new StatText("Speed", decimal(value * TickRate.TICKS_PER_SECOND) + " px/s");
            case PHYSICAL_DAMAGE_TAKEN -> new StatText("Physical taken", percent(value));
            case MAGIC_DAMAGE_TAKEN -> new StatText("Magic taken", percent(value));
            case RESILIENCE -> value >= 100f ? new StatText("Crit immune", "")
                    : new StatText("Resilience", "-" + percent(value / 100f) + " crits");
            case CRIT_CHANCE_TAKEN -> new StatText("Crit chance taken", "x" + decimal(value));
            case SPIRIT -> new StatText("Spirit", signedPercent(value / 100f) + " heals");
            case REGENERATION -> new StatText("Regenerates", decimal(value / 100f * TickRate.TICKS_PER_SECOND) + "/s");
            case SLOW_RESIST -> resistText("Slow", value);
            case BURN_RESIST -> resistText("Burn", value);
            case FREEZE_RESIST -> resistText("Freeze", value);
            case STEALTH -> new StatText("Stealthed", "");
            case FREEZE_DR -> new StatText("Freeze DR", live && value >= 1f && inspection.freezeStep() > 0
                    ? "next " + freezeStepText(inspection.freezeStep()) : "");
        };
    }

    private static Row effectRow(EnemyInspection.EffectState effect) {
        String name = switch (effect.kind()) {
            case SLOW -> "Slowed";
            case BURN -> "Burning";
            case FREEZE -> "Frozen";
            case SHIELD -> "Shielded";
            case INVISIBLE -> "Invisible";
            case HEAL -> "Healing";
        };
        String left = effect.remainingTicks().isEmpty() ? ""
                : String.format(Locale.ROOT, "%.1f s", effect.remainingTicks().getAsInt() / TickRate.TICKS_PER_SECOND);
        return Row.effect(EnemyFrameBuilder.markerPaletteFor(effect.kind()), name, left);
    }

    private static String freezeStepText(int step) {
        return switch (step) {
            case 0 -> "100%";
            case 1 -> "50%";
            case 2 -> "25%";
            default -> "blocked";
        };
    }

    private static StatText resistText(String kind, float value) {
        return value >= 1f ? new StatText(kind + " immune", "") : new StatText(kind + " resist", percent(value));
    }

    private static String platingText(float value) {
        return "-" + decimal(value / 100f) + "/hit";
    }

    /** "-50%" for armor that halves a hit, "+25%" for negative armor. */
    private static String mitigationText(float armor) {
        return signedPercent(HitResolution.mitigationMultiplier(armor) - 1f);
    }

    private static String percent(float fraction) {
        return Math.round(fraction * 100) + "%";
    }

    private static String signedPercent(float fraction) {
        int rounded = Math.round(fraction * 100);
        return (rounded > 0 ? "+" : "") + rounded + "%";
    }

    private static String decimal(float value) {
        if (Math.abs(value - Math.round(value)) < 0.05f) {
            return Integer.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static String titleCase(Rank rank) {
        String name = rank.name();
        return name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT);
    }

    /** A stat row's words before its colour is known. */
    record StatText(String label, String value) {
    }
}
