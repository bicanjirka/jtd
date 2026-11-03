package td.ui;

import td.damage.DamageType;
import td.economy.StatusTerm;
import td.effect.EffectKind;
import td.enemy.EnemyInspection;
import td.enemy.EnemyMark;
import td.enemy.StatRows;
import td.enemy.TraitEntry;
import td.stat.EnemyStat;
import td.stat.StatBreakdown;
import td.stat.StatWording;
import td.term.Side;
import td.term.Term;
import td.term.TermNumbers;
import td.term.TermText;
import td.ui.render.IconFace;
import td.ui.render.InfoSheet;
import td.ui.render.SheetIcon;
import td.ui.render.SheetLine;
import td.ui.render.SheetLine.Glyph;
import td.ui.render.SheetLine.Row;
import td.ui.render.TooltipKey;
import td.util.TickRate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
    static InfoSheet preview(EnemyInspection inspection, StatWording wording) {
        List<SheetLine> lines = new ArrayList<>();
        lines.add(header(inspection));
        lines.add(new SheetLine.HealthBar(inspection.maxHealth(), inspection.maxHealth(), tallies(inspection),
                Optional.empty()));
        lines.add(new SheetLine.Gap());
        lines.addAll(StatRowText.catalog(breakdowns(inspection), wording));
        iconRow("Traits", traitIcons(inspection)).ifPresent(lines::add);
        role(inspection, lines);
        return new InfoSheet(lines);
    }

    /** The live inspector: health and fate, stats with breakdowns, the three icon rows, the role sentence. */
    static InfoSheet live(EnemyInspection inspection, StatWording wording) {
        List<SheetLine> lines = new ArrayList<>();
        lines.add(header(inspection));
        lines.add(new SheetLine.HealthBar(inspection.health(), inspection.maxHealth(), tallies(inspection),
                switch (inspection.fate()) {
                    case ALIVE -> Optional.empty();
                    case KILLED -> Optional.of("Killed");
                    case LEAKED -> Optional.of("Leaked");
                    case HATCHED -> Optional.of("Hatched");
                }).withShields(shieldsOf(inspection)));
        lines.add(new SheetLine.Gap());
        lines.addAll(StatRowText.live(breakdowns(inspection), wording));
        if (inspection.dodges() > 0) {
            lines.add(Row.plain(Glyph.DOT, "Dodged", TermNumbers.number(inspection.dodges()))
                    .withGloss(inspection.dodges() == 1 ? "hit" : "hits", ""));
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
                TermNumbers.titleCase(inspection.rank()));
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

    /** What it is worth: the bounty a kill pays and the lives a leak costs, each with its status-bar face. */
    private static List<SheetLine.Tally> tallies(EnemyInspection inspection) {
        return List.of(new SheetLine.Tally(TermLooks.faceOf(StatusTerm.CASH), TermNumbers.number(inspection.bounty())),
                new SheetLine.Tally(TermLooks.faceOf(StatusTerm.LIVES), inspection.takesAllLives() ? "all"
                        : TermNumbers.number(inspection.livesOnLeak())));
    }

    /** Every stat that shows, each with what made it; one nothing broke down reads as unchanged. */
    private static List<StatBreakdown> breakdowns(EnemyInspection inspection) {
        List<StatBreakdown> shown = new ArrayList<>();
        for (EnemyStat stat : EnemyStat.values()) {
            float base = inspection.baseStats().get(stat);
            float current = inspection.stat(stat);
            if (StatRows.shows(stat, base, current)) {
                StatBreakdown breakdown = inspection.breakdowns().get(stat);
                shown.add(breakdown != null ? breakdown
                        : new StatBreakdown(stat, base, List.of(), base, List.of(), current));
            }
        }
        return shown;
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
            rows.add(Row.plain(Glyph.DOT, "Time left",
                    TermNumbers.number(effect.remainingTicks().getAsInt() / TickRate.TICKS_PER_SECOND))
                    .withGloss("s", ""));
        }
        if (kind.maxStacks() > 0 && effect.stacks() > 0) {
            rows.add(Row.plain(Glyph.DOT, "Stacks", effect.stacks() + " / " + kind.maxStacks()));
        }
        for (EnemyInspection.ShareState share : effect.shares()) {
            rows.add(kind == EffectKind.SHIELD ? shieldShareRow(share) : healShareRow(share));
        }
        if (kind == EffectKind.CHILL) {
            rows.add(Row.plain(Glyph.DOT, "Slows", TermNumbers.percent(effect.level())));
        }
        effect.nextShare().ifPresent(share -> rows.add(SheetLine.Sentence.of(TermText.of(share > 0
                ? "the next one lasts " + TermNumbers.percent((float) share) : "the next one is kept out")
                .asSentence())));
        return rows;
    }

    /** One source's shield: what it holds of what it was cast with, what it takes, and how long it has. */
    private static Row shieldShareRow(EnemyInspection.ShareState share) {
        String label = share.restrictedTo().map(type -> switch (type) {
            case PHYSICAL -> "Holds physical";
            case MAGIC -> "Holds magic";
        }).orElse("Holds");
        return Row.plain(Glyph.DOT, label, TermNumbers.number(share.amount()) + " / " + TermNumbers.number(share.max()))
                .withGloss("hp", timeLeft(share));
    }

    /** One source's heal: the regeneration it gives, and how long it has. */
    private static Row healShareRow(EnemyInspection.ShareState share) {
        return Row.plain(Glyph.DOT, "Gives", TermNumbers.number(share.amount())).withGloss("hp/s", timeLeft(share));
    }

    private static String timeLeft(EnemyInspection.ShareState share) {
        return share.remainingTicks().isPresent()
                ? TermNumbers.number(share.remainingTicks().getAsInt() / TickRate.TICKS_PER_SECOND) + " s"
                : "until broken";
    }

    /** What the enemy's shields hold, by the damage they take, for the strip under its health. */
    private static SheetLine.Shields shieldsOf(EnemyInspection inspection) {
        float any = 0f;
        float physical = 0f;
        float magic = 0f;
        for (EnemyInspection.EffectState effect : inspection.effects()) {
            if (effect.kind() != EffectKind.SHIELD || effect.faded()) {
                continue;
            }
            for (EnemyInspection.ShareState share : effect.shares()) {
                if (share.restrictedTo().isEmpty()) {
                    any += share.amount();
                } else if (share.restrictedTo().get() == DamageType.PHYSICAL) {
                    physical += share.amount();
                } else {
                    magic += share.amount();
                }
            }
        }
        return new SheetLine.Shields(any, physical, magic);
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
}
