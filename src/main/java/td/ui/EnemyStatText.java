package td.ui;

import td.damage.DamageUnits;
import td.effect.EffectKind;
import td.enemy.EnemyInspection;
import td.enemy.HitResolution;
import td.enemy.TraitLine;
import td.stat.EnemyStat;
import td.ui.render.InfoSheet;
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
    static InfoSheet preview(EnemyInspection inspection) {
        List<SheetLine> lines = new ArrayList<>();
        lines.add(header(inspection));
        lines.add(new SheetLine.HealthBar(inspection.maxHealth(), inspection.maxHealth(), "$" + inspection.bounty(), false));
        lines.add(new SheetLine.Gap());
        lines.addAll(statAndTraitRows(inspection));
        if (!inspection.description().isEmpty()) {
            lines.add(new SheetLine.Gap());
            lines.add(new SheetLine.Prose(inspection.description()));
        }
        return new InfoSheet(lines);
    }

    /** The live inspector: current health and fate, stats, traits, then effects with time left. */
    static InfoSheet live(EnemyInspection inspection) {
        List<SheetLine> lines = new ArrayList<>();
        lines.add(header(inspection));
        lines.add(switch (inspection.fate()) {
            case ALIVE -> new SheetLine.HealthBar(inspection.health(), inspection.maxHealth(), "$" + inspection.bounty(), false);
            case KILLED -> new SheetLine.HealthBar(inspection.health(), inspection.maxHealth(), "Killed", true);
            case LEAKED -> new SheetLine.HealthBar(inspection.health(), inspection.maxHealth(), "Leaked", true);
        });
        lines.add(new SheetLine.Gap());
        lines.addAll(statAndTraitRows(inspection));
        inspection.effects().forEach(effect -> lines.add(effectRow(effect)));
        inspection.blocked().forEach(kind -> lines.add(Row.effect(EnemyFrameBuilder.markerPaletteFor(kind),
                "Immune to " + kind.name().toLowerCase(Locale.ROOT), "")));
        if (inspection.freezeStep() > 0) {
            lines.add(Row.plain(Glyph.DOT, "Freeze DR", "next " + freezeStepText(inspection.freezeStep())));
        }
        return new InfoSheet(lines);
    }

    private static SheetLine.EnemyHeader header(EnemyInspection inspection) {
        return new SheetLine.EnemyHeader(EnemyFrameBuilder.paletteFor(inspection.archetype()),
                EnemyFrameBuilder.badgeFor(inspection.rank()), inspection.name(), SheetNumbers.titleCase(inspection.rank()));
    }

    /** Speed first, which always shows, then every other stat off its default, then traits no stat covers. */
    private static List<Row> statAndTraitRows(EnemyInspection inspection) {
        List<Row> rows = new ArrayList<>();
        StatText speed = statText(EnemyStat.MOVE_SPEED, inspection.stat(EnemyStat.MOVE_SPEED));
        rows.add(Row.plain(Glyph.CHEVRON, speed.label(), speed.value()));
        Set<EnemyStat> shown = EnumSet.noneOf(EnemyStat.class);
        for (EnemyStat stat : EnemyStat.values()) {
            if (stat == EnemyStat.MOVE_SPEED || shown.contains(stat) || isShielding(stat)
                    || !differsFromDefault(inspection, stat)) {
                continue;
            }
            Optional<EnemyStat> pair = pairedWith(stat).filter(other -> inspection.stat(other) == inspection.stat(stat));
            shown.add(stat);
            pair.ifPresent(shown::add);
            StatText text = pair.isPresent() ? pairText(stat, inspection.stat(stat)) : statText(stat, inspection.stat(stat));
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

    /** A shield has its own effect row, so its stat needs none. */
    private static boolean isShielding(EnemyStat stat) {
        return stat == EnemyStat.PHYSICAL_SHIELDING || stat == EnemyStat.MAGIC_SHIELDING;
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
        return new StatText("Resist all", mitigationText(value));
    }

    static StatText statText(EnemyStat stat, float value) {
        return switch (stat) {
            case ARMOR -> new StatText("Resist physical", mitigationText(value));
            case MAGIC_RESIST -> new StatText("Resist magic", mitigationText(value));
            case PHYSICAL_PLATING -> new StatText("Plating, physical", platingText(value));
            case MAGIC_PLATING -> new StatText("Plating, magic", platingText(value));
            case MOVE_SPEED -> new StatText("Speed", SheetNumbers.decimal(value * TickRate.TICKS_PER_SECOND) + " px/s");
            case PHYSICAL_DAMAGE_TAKEN -> new StatText("Physical taken", SheetNumbers.percent(value));
            case MAGIC_DAMAGE_TAKEN -> new StatText("Magic taken", SheetNumbers.percent(value));
            case PHYSICAL_SHIELDING -> new StatText("Shielded, physical", SheetNumbers.percent(value));
            case MAGIC_SHIELDING -> new StatText("Shielded, magic", SheetNumbers.percent(value));
            case RESILIENCE -> resilienceText(value);
            case CRIT_CHANCE_TAKEN -> new StatText("Crit chance taken", "x" + SheetNumbers.decimal(value));
            case SPIRIT -> new StatText("Spirit", SheetNumbers.signedPercent(value / 100f) + " heals" + debuffPaceText(value));
            case REGENERATION -> new StatText("Regenerates", SheetNumbers.decimal(DamageUnits.inPoints(value) * TickRate.TICKS_PER_SECOND) + "/s");
            case CHILL_RESIST -> resistText("Chill", value);
            case BURN_RESIST -> resistText("Burn", value);
            case FREEZE_RESIST -> resistText("Freeze", value);
            case STEALTH -> new StatText("Stealthed", "");
        };
    }

    /** What spirit does to debuff timers, in words; empty while it barely changes them. */
    private static String debuffPaceText(float spirit) {
        float pace = Math.max(0.25f, 1f + spirit / 100f);
        if (Math.abs(pace - 1f) < 0.05f) {
            return "";
        }
        return ", debuffs " + SheetNumbers.decimal(pace > 1f ? pace : 1f / pace) + "x "
                + (pace > 1f ? "faster" : "slower");
    }

    private static StatText resilienceText(float value) {
        if (value >= 100f) {
            return new StatText("Crit immune", "");
        }
        return value < 0f
                ? new StatText("Resilience", "+" + SheetNumbers.percent(-value / 100f) + " crit damage")
                : new StatText("Resilience", "-" + SheetNumbers.percent(value / 100f) + " crits");
    }

    private static Row effectRow(EnemyInspection.EffectState effect) {
        String name = switch (effect.kind()) {
            case CHILL -> "Chilled";
            case BURN -> "Burning";
            case FREEZE -> "Frozen";
            case SHIELD -> "Shielded";
            case INVISIBLE -> "Invisible";
            case HEAL -> "Healing";
            case VULNERABLE -> "Vulnerable x" + effect.stacks();
            case REVEALED -> "Revealed";
            case POISON -> "Poisoned";
            case SCORCHED -> "Scorched x" + effect.stacks();
            case SICKENED -> "Sickened x" + effect.stacks();
            case SUNDERED -> "Sundered x" + effect.stacks();
            case EXPOSED -> "Exposed";
            case MARKED -> "Marked";
            case PRIORITY -> "Priority";
            case RESONATING -> "Resonating x" + effect.stacks();
            case FRACTURED -> "Fractured x" + effect.stacks();
        };
        String category = effect.kind().category().label();
        if (effect.kind() == EffectKind.CHILL) {
            category += " " + SheetNumbers.percent(effect.level());
        }
        String left = effect.remainingTicks().isEmpty() ? category
                : category + " " + String.format(Locale.ROOT, "%.1f s", effect.remainingTicks().getAsInt() / TickRate.TICKS_PER_SECOND);
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
        return value >= 1f ? new StatText(kind + " immune", "") : new StatText(kind + " resist", SheetNumbers.percent(value));
    }

    private static String platingText(float value) {
        return "-" + SheetNumbers.decimal(DamageUnits.inPoints(value)) + "/hit";
    }

    /** "-50%" for armor that halves a hit. */
    private static String mitigationText(float armor) {
        return SheetNumbers.signedPercent(HitResolution.mitigationMultiplier(armor) - 1f);
    }

    /** A stat row's words before its colour is known. */
    record StatText(String label, String value) {
    }
}
