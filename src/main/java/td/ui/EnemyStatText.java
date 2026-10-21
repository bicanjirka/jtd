package td.ui;

import td.damage.DamageUnits;
import td.effect.EffectKind;
import td.enemy.EnemyInspection;
import td.enemy.EnemyMark;
import td.enemy.HitResolution;
import td.enemy.StatBreakdown;
import td.enemy.StatRows;
import td.enemy.TraitEntry;
import td.stat.EnemyStat;
import td.stat.StatModifier;
import td.term.Side;
import td.term.Term;
import td.term.TermText;
import td.ui.render.IconFace;
import td.ui.render.InfoSheet;
import td.ui.render.SheetIcon;
import td.ui.render.SheetLine;
import td.ui.render.SheetLine.Glyph;
import td.ui.render.SheetLine.Row;
import td.ui.render.SheetLine.Trend;
import td.ui.render.TooltipKey;
import td.util.TickRate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Turns an {@link EnemyInspection} into the enemy pane: stat rows, then the Traits, Buffs and Debuffs icon rows,
 * then its role sentence. Pure: no Swing, no live mob.
 */
final class EnemyStatText {

    private static final int TIME_LABEL_BELOW_TICKS = Math.round(10 * TickRate.TICKS_PER_SECOND);

    private EnemyStatText() {
    }

    /** The wave-preview hover: identity, full health, its stats, the Traits row and its role sentence. */
    static InfoSheet preview(EnemyInspection inspection) {
        List<SheetLine> lines = new ArrayList<>();
        lines.add(header(inspection));
        lines.add(new SheetLine.HealthBar(inspection.maxHealth(), inspection.maxHealth(), "$" + inspection.bounty(),
                false));
        lines.add(new SheetLine.Gap());
        lines.addAll(statRows(inspection, false));
        iconRow("Traits", traitIcons(inspection)).ifPresent(lines::add);
        role(inspection, lines);
        return new InfoSheet(lines);
    }

    /** The live inspector: health and fate, stats with breakdowns, the three icon rows, the role sentence. */
    static InfoSheet live(EnemyInspection inspection) {
        List<SheetLine> lines = new ArrayList<>();
        lines.add(header(inspection));
        lines.add(switch (inspection.fate()) {
            case ALIVE -> new SheetLine.HealthBar(inspection.health(), inspection.maxHealth(),
                    "$" + inspection.bounty(), false);
            case KILLED -> new SheetLine.HealthBar(inspection.health(), inspection.maxHealth(), "Killed", true);
            case LEAKED -> new SheetLine.HealthBar(inspection.health(), inspection.maxHealth(), "Leaked", true);
            case HATCHED -> new SheetLine.HealthBar(inspection.health(), inspection.maxHealth(), "Hatched", true);
        });
        lines.add(new SheetLine.Gap());
        lines.addAll(statRows(inspection, true));
        if (inspection.dodges() > 0) {
            lines.add(Row.plain(Glyph.DOT, "Dodged",
                    inspection.dodges() == 1 ? "1 hit" : inspection.dodges() + " hits"));
        }
        iconRow("Traits", traitIcons(inspection)).ifPresent(lines::add);
        iconRow("Buffs", effectIcons(inspection, Side.BUFF)).ifPresent(lines::add);
        iconRow("Debuffs", debuffIcons(inspection)).ifPresent(lines::add);
        role(inspection, lines);
        return new InfoSheet(lines);
    }

    private static SheetLine.EnemyHeader header(EnemyInspection inspection) {
        return new SheetLine.EnemyHeader(EnemyFrameBuilder.paletteFor(inspection.archetype()),
                EnemyFrameBuilder.badgeFor(inspection.rank()), inspection.name(),
                SheetNumbers.titleCase(inspection.rank()));
    }

    private static Optional<SheetLine> iconRow(String label, List<SheetIcon> icons) {
        return icons.isEmpty() ? Optional.empty() : Optional.of(new SheetLine.IconRow(label, icons));
    }

    private static void role(EnemyInspection inspection, List<SheetLine> lines) {
        if (!inspection.description().isEmpty()) {
            lines.add(new SheetLine.Gap());
            lines.add(new SheetLine.Prose(TermText.of(inspection.description())));
        }
    }

    /** Speed first, then every other stat that shows, in enum order; a live row opens what it is made of. */
    private static List<SheetLine> statRows(EnemyInspection inspection, boolean live) {
        List<SheetLine> rows = new ArrayList<>();
        List<EnemyStat> order = new ArrayList<>();
        order.add(EnemyStat.MOVE_SPEED);
        for (EnemyStat stat : EnemyStat.values()) {
            if (stat != EnemyStat.MOVE_SPEED) {
                order.add(stat);
            }
        }
        for (EnemyStat stat : order) {
            float base = inspection.baseStats().get(stat);
            float current = inspection.stat(stat);
            if (!StatRows.shows(stat, base, current)) {
                continue;
            }
            StatText text = statText(stat, current);
            Row row = Row.plain(stat == EnemyStat.MOVE_SPEED ? Glyph.CHEVRON : Glyph.DOT, text.label(), text.value())
                    .withTrend(trend(stat, base, current));
            StatBreakdown breakdown = inspection.breakdowns().get(stat);
            rows.add(live && breakdown != null
                    ? row.withTooltip(TooltipKey.of("stat", stat.name()), breakdownSheet(breakdown))
                    : row);
        }
        return rows;
    }

    private static Trend trend(EnemyStat stat, float base, float current) {
        if (current == base) {
            return Trend.NONE;
        }
        return (current < base) == lowerIsBetterForPlayer(stat) ? Trend.BETTER : Trend.WORSE;
    }

    /** Whether a lower {@code stat} is better for the player: a slower, softer, less evasive enemy. */
    static boolean lowerIsBetterForPlayer(EnemyStat stat) {
        return switch (stat) {
            case ARMOR, MAGIC_RESIST, PLATING, INSULATION, MOVE_SPEED, RESILIENCE, AGILITY, SPIRIT, HEALING_RECEIVED,
                    REGENERATION, CHILL_RESIST, BURN_RESIST, FREEZE_RESIST, STEALTH -> true;
            case PHYSICAL_DAMAGE_TAKEN, MAGIC_DAMAGE_TAKEN, PERIODIC_DAMAGE_TAKEN -> false;
        };
    }

    private static InfoSheet breakdownSheet(StatBreakdown breakdown) {
        EnemyStat stat = breakdown.stat();
        List<SheetLine> lines = new ArrayList<>();
        lines.add(Row.plain(Glyph.DOT, statText(stat, breakdown.current()).label(), ""));
        lines.add(Row.plain(Glyph.DOT, "Base", statText(stat, breakdown.base()).value()));
        breakdown.parts().forEach(part -> lines.add(SheetLine.Sentence.of(TermText.of(part.source())
                .plus(": " + modifierText(stat, part.modifier())))));
        lines.add(Row.plain(Glyph.DOT, "Now", statText(stat, breakdown.current()).value()));
        return new InfoSheet(lines);
    }

    /** What one source does to {@code stat}, in the stat's own unit: "-10", "x0.5", "= 0", "at most 75% of base". */
    static String modifierText(EnemyStat stat, StatModifier modifier) {
        List<String> parts = new ArrayList<>();
        if (modifier.hasSetValue()) {
            parts.add("= " + statText(stat, modifier.setTo()).value());
        }
        if (modifier.flat() != 0f) {
            parts.add(signedIn(stat, modifier.flat()));
        }
        if (modifier.percentAdd() != 0f) {
            parts.add(SheetNumbers.signedPercent(modifier.percentAdd()));
        }
        if (modifier.multiply() != 1f) {
            parts.add("x" + SheetNumbers.decimal(modifier.multiply()));
        }
        if (modifier.hasCap()) {
            parts.add("at most " + SheetNumbers.percent(modifier.capShareOfBase()) + " of base");
        }
        return String.join(", ", parts);
    }

    private static String signedIn(EnemyStat stat, float amount) {
        String sign = amount > 0f ? "+" : "";
        return switch (stat) {
            case PHYSICAL_DAMAGE_TAKEN, MAGIC_DAMAGE_TAKEN, PERIODIC_DAMAGE_TAKEN, HEALING_RECEIVED, CHILL_RESIST,
                    BURN_RESIST, FREEZE_RESIST -> SheetNumbers.signedPercent(amount);
            case PLATING, INSULATION -> sign + SheetNumbers.decimal(DamageUnits.inPoints(amount));
            case REGENERATION -> sign + SheetNumbers.decimal(DamageUnits.inPoints(amount) * TickRate.TICKS_PER_SECOND)
                    + "/s";
            case MOVE_SPEED -> sign + SheetNumbers.decimal(amount * TickRate.TICKS_PER_SECOND) + " px/s";
            case ARMOR, MAGIC_RESIST, RESILIENCE, AGILITY, SPIRIT, STEALTH -> sign + Math.round(amount);
        };
    }

    private static List<SheetIcon> traitIcons(EnemyInspection inspection) {
        List<SheetIcon> icons = new ArrayList<>();
        List<TraitEntry> traits = inspection.traitEntries();
        for (int i = 0; i < traits.size(); i++) {
            TraitEntry entry = traits.get(i);
            IconFace face = TermLooks.faceOf(entry.term());
            if (entry.immune()) {
                face = face.withWard(IconFace.Ward.IMMUNE);
            } else if (entry.resisted() > 0f) {
                face = face.withWard(IconFace.Ward.RESISTED);
            }
            icons.add(new SheetIcon(face, TooltipKey.ofTerm("trait", i, entry.term()),
                    TermSheets.of(entry.term(), entry.text(), List.of())));
        }
        for (int i = 0; i < inspection.abilityEntries().size(); i++) {
            var entry = inspection.abilityEntries().get(i);
            icons.add(new SheetIcon(TermLooks.faceOf(entry.term()), TooltipKey.ofTerm("ability", i, entry.term()),
                    TermSheets.of(entry.term(), entry.text(), List.of())));
        }
        for (EnemyInspection.Immunity immunity : inspection.immunities()) {
            boolean shown = traits.stream().anyMatch(entry -> entry.immune() && entry.term() == immunity.kind());
            if (immunity.keptOutBy().isEmpty() || shown) {
                continue;
            }
            EffectKind kind = immunity.kind();
            icons.add(new SheetIcon(TermLooks.faceOf(kind).withWard(IconFace.Ward.IMMUNE),
                    TooltipKey.ofTerm("immune", kind),
                    TermSheets.of(kind, TermText.format("immune to {} while it has {}", kind,
                            immunity.keptOutBy().get()), List.of())));
        }
        return icons;
    }

    private static List<SheetIcon> effectIcons(EnemyInspection inspection, Side side) {
        return inspection.effects().stream()
                .filter(effect -> effect.kind().side() == side)
                .map(EnemyStatText::effectIcon)
                .toList();
    }

    private static SheetIcon effectIcon(EnemyInspection.EffectState effect) {
        EffectKind kind = effect.kind();
        IconFace face = TermLooks.faceOf(kind);
        if (effect.faded()) {
            face = face.asFaded();
        }
        if (kind == EffectKind.DRAGGED) {
            face = effect.stacks() > 0 ? face.withCorner(effect.stacks() + "%") : face;
        } else if (kind.maxStacks() > 0 && effect.stacks() > 1) {
            face = face.withCorner(Integer.toString(effect.stacks()));
        }
        if (effect.remainingTicks().isPresent() && effect.clockTicks().isPresent() && effect.clockTicks().getAsInt() > 0) {
            int remaining = effect.remainingTicks().getAsInt();
            face = face.withElapsed(1f - (float) remaining / effect.clockTicks().getAsInt());
        }
        if (effect.remainingTicks().isPresent() && effect.remainingTicks().getAsInt() < TIME_LABEL_BELOW_TICKS) {
            face = face.withBelow((int) Math.ceil(effect.remainingTicks().getAsInt() / (double) TickRate.TICKS_PER_SECOND)
                    + "s");
        }
        if (effect.fullness().isPresent()) {
            face = face.withBar(effect.fullness().getAsDouble());
        } else if (kind == EffectKind.SHIELD && effect.shield().max() > 0f) {
            face = face.withBar(effect.shield().remaining() / effect.shield().max());
        }
        return new SheetIcon(face, TooltipKey.ofTerm("effect", kind),
                TermSheets.of(kind, kind.describe(), effectRows(effect)));
    }

    private static List<SheetLine> effectRows(EnemyInspection.EffectState effect) {
        EffectKind kind = effect.kind();
        List<SheetLine> rows = new ArrayList<>();
        if (effect.remainingTicks().isPresent()) {
            rows.add(Row.plain(Glyph.DOT, "Time left", String.format(Locale.ROOT, "%.1f s",
                    effect.remainingTicks().getAsInt() / (double) TickRate.TICKS_PER_SECOND)));
        }
        if (kind.maxStacks() > 0 && effect.stacks() > 0) {
            rows.add(Row.plain(Glyph.DOT, "Stacks", effect.stacks() + " / " + kind.maxStacks()));
        }
        if (kind == EffectKind.SHIELD) {
            rows.add(Row.plain(Glyph.DOT, "Absorbs", Math.round(effect.shield().remaining()) + " / "
                    + Math.round(effect.shield().max())));
        }
        if (kind == EffectKind.CHILL) {
            rows.add(Row.plain(Glyph.DOT, "Slows", SheetNumbers.percent(effect.level())));
        }
        effect.nextShare().ifPresent(share -> rows.add(SheetLine.Sentence.of(TermText.of(share > 0
                ? "the next one lasts " + SheetNumbers.percent((float) share) : "the next one is kept out")
                .asSentence())));
        return rows;
    }

    /** The debuffs, then each term towers hold a mark of, once, with its highest step. */
    private static List<SheetIcon> debuffIcons(EnemyInspection inspection) {
        List<SheetIcon> icons = new ArrayList<>(effectIcons(inspection, Side.DEBUFF));
        Map<Term, List<EnemyMark>> byTerm = new LinkedHashMap<>();
        inspection.marks().forEach(mark -> byTerm.computeIfAbsent(mark.term(), term -> new ArrayList<>()).add(mark));
        byTerm.forEach((term, marks) -> {
            int highest = marks.stream().mapToInt(EnemyMark::steps).max().orElse(0);
            List<SheetLine> rows = marks.stream().map(mark -> (SheetLine) Row.plain(Glyph.DOT, mark.source(),
                    mark.steps() + " / " + mark.maxSteps())).toList();
            icons.add(new SheetIcon(TermLooks.faceOf(term).withCorner(Integer.toString(highest)),
                    TooltipKey.ofTerm("mark", term), TermSheets.of(term, term.describe(), rows)));
        });
        return icons;
    }

    static StatText statText(EnemyStat stat, float value) {
        return switch (stat) {
            case ARMOR -> new StatText("Armor", Integer.toString(Math.round(value)));
            case MAGIC_RESIST -> new StatText("Magic resist", Integer.toString(Math.round(value)));
            case PLATING -> new StatText("Plating", platingText(value));
            case INSULATION -> new StatText("Insulation", platingText(value));
            case MOVE_SPEED -> new StatText("Speed",
                    SheetNumbers.decimal(value * TickRate.TICKS_PER_SECOND) + " px/s");
            case PHYSICAL_DAMAGE_TAKEN -> new StatText("Physical taken", SheetNumbers.percent(value));
            case MAGIC_DAMAGE_TAKEN -> new StatText("Magic taken", SheetNumbers.percent(value));
            case RESILIENCE -> resilienceText(value);
            case AGILITY -> new StatText("Agility", Math.round(value) + ", dodges "
                    + SheetNumbers.percent(HitResolution.dodgeChance(value)));
            case PERIODIC_DAMAGE_TAKEN -> new StatText("Over time taken", SheetNumbers.percent(value));
            case HEALING_RECEIVED -> new StatText("Heals received", SheetNumbers.percent(value));
            case SPIRIT -> new StatText("Spirit",
                    SheetNumbers.signedPercent(value / 100f) + " heals" + debuffPaceText(value));
            case REGENERATION -> new StatText("Regenerates",
                    SheetNumbers.decimal(DamageUnits.inPoints(value) * TickRate.TICKS_PER_SECOND) + "/s");
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
                ? new StatText("Resilience", "+" + SheetNumbers.percent(-value / 100f) + " crits")
                : new StatText("Resilience", "-" + SheetNumbers.percent(value / 100f) + " crits");
    }

    private static StatText resistText(String kind, float value) {
        return value >= 1f ? new StatText(kind + " immune", "")
                : new StatText(kind + " resist", SheetNumbers.percent(value));
    }

    private static String platingText(float value) {
        return "-" + SheetNumbers.decimal(DamageUnits.inPoints(value)) + "/hit";
    }

    /** A stat row's words before its colour is known. */
    record StatText(String label, String value) {
    }
}
