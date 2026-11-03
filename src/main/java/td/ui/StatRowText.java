package td.ui;

import td.stat.StatBreakdown;
import td.stat.StatDirection;
import td.stat.StatGroup;
import td.stat.StatModifier;
import td.stat.StatPart;
import td.stat.StatPreview;
import td.stat.StatReading;
import td.stat.StatTerm;
import td.stat.StatWording;
import td.term.Term;
import td.term.TermText;
import td.ui.StatLooks.StatLook;
import td.ui.render.InfoSheet;
import td.ui.render.SheetLine;
import td.ui.render.SheetLine.Arrow;
import td.ui.render.SheetLine.Glyph;
import td.ui.render.SheetLine.Row;
import td.ui.render.SheetLine.Trend;
import td.ui.render.TooltipKey;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Every stat row on every sheet, towers and enemies, live and catalog alike: the stat's mark, its short name, its
 * value with unit and meaning, and a hover with its full name, what it does and, live, what made it. Pure.
 */
final class StatRowText {

    private static final String STACKS = " ×";

    private StatRowText() {
    }

    /** A live entity's rows: each value now, judged against its base, with what made it in the hover. */
    static List<SheetLine> live(List<StatBreakdown> stats, StatWording wording) {
        return grouped(stats, breakdown -> liveRow(breakdown, wording));
    }

    /** A catalog entity's rows: each base value, unjudged, the hover saying only what the stat does. */
    static List<SheetLine> catalog(List<StatBreakdown> stats, StatWording wording) {
        return grouped(stats, breakdown -> valueRow(breakdown.stat(), breakdown.base(), wording)
                .withTooltip(key(breakdown.stat()), meaningSheet(breakdown.stat())));
    }

    /** A row stating what {@code modifier} does to {@code stat}: "Fire rate +25%". */
    static Row change(StatTerm stat, StatModifier modifier, StatWording wording) {
        return marked(stat, wording.change(stat, modifier)).withTooltip(key(stat), meaningSheet(stat));
    }

    /** A change, then what the row it moves would read: "Fire rate +25%  1.3 → 1.6 /s". */
    static Row change(StatTerm stat, StatModifier modifier, StatPreview preview, StatWording wording) {
        StatReading before = wording.read(preview.stat(), preview.before());
        StatReading after = wording.read(preview.stat(), preview.after());
        String shift = before.number() + " → " + after.number();
        return change(stat, modifier, wording).withGloss(after.unit(), shift)
                .withArrow(arrow(before, after, preview.before(), preview.after()));
    }

    /** What hovering a stat or a mention of one opens: its mark and full name, and what it does. */
    static InfoSheet meaningSheet(StatTerm stat) {
        List<SheetLine> lines = new ArrayList<>();
        lines.add(title(stat));
        lines.add(SheetLine.Sentence.of(stat.describe().asSentence()));
        return new InfoSheet(lines);
    }

    /** Rows in group order, a gap between two groups, in the given order within one. */
    private static List<SheetLine> grouped(List<StatBreakdown> stats, Function<StatBreakdown, Row> row) {
        List<StatBreakdown> sorted = stats.stream()
                .sorted(Comparator.comparing((StatBreakdown breakdown) -> breakdown.stat().group())).toList();
        List<SheetLine> lines = new ArrayList<>();
        Optional<StatGroup> last = Optional.empty();
        for (StatBreakdown breakdown : sorted) {
            StatGroup group = breakdown.stat().group();
            if (last.isPresent() && last.get() != group) {
                lines.add(new SheetLine.Gap());
            }
            last = Optional.of(group);
            lines.add(row.apply(breakdown));
        }
        return lines;
    }

    private static Row liveRow(StatBreakdown breakdown, StatWording wording) {
        StatTerm stat = breakdown.stat();
        StatReading base = wording.read(stat, breakdown.base());
        StatReading now = wording.read(stat, breakdown.current());
        Row row = valueRow(stat, breakdown.current(), wording)
                .withArrow(arrow(base, now, breakdown.base(), breakdown.current()));
        if (!base.number().equals(now.number())) {
            row = row.withTrend(trend(stat.direction(), breakdown.current() > breakdown.base()));
        }
        return row.withTooltip(key(stat), breakdownSheet(breakdown, wording));
    }

    private static Row valueRow(StatTerm stat, float value, StatWording wording) {
        StatReading reading = wording.read(stat, value);
        return marked(stat, reading.number()).withGloss(reading.unit(), reading.meaning().orElse(""));
    }

    private static Row marked(StatTerm stat, String value) {
        StatLook look = StatLooks.of(stat);
        return new Row(look.glyph(), look.tone(), look.face(), stat.shortName(), value, "", "", Trend.NONE,
                Arrow.NONE, Optional.empty());
    }

    /** Which way the shown number moved; none when it reads the same. */
    private static Arrow arrow(StatReading before, StatReading after, float from, float to) {
        if (before.number().equals(after.number())) {
            return Arrow.NONE;
        }
        return to > from ? Arrow.UP : Arrow.DOWN;
    }

    private static Trend trend(StatDirection direction, boolean rose) {
        return switch (direction) {
            case HIGHER -> rose ? Trend.BETTER : Trend.WORSE;
            case LOWER -> rose ? Trend.WORSE : Trend.BETTER;
            case NEUTRAL -> Trend.NONE;
        };
    }

    private static TooltipKey key(StatTerm stat) {
        return TooltipKey.of("stat", stat.key());
    }

    private static SheetLine.StatTitle title(StatTerm stat) {
        StatLook look = StatLooks.of(stat);
        return new SheetLine.StatTitle(look.glyph(), look.tone(), look.face(), stat.title());
    }

    /** The meaning, then Base, each source that adds, what they come to together, each that scales, and Now. */
    private static InfoSheet breakdownSheet(StatBreakdown breakdown, StatWording wording) {
        StatTerm stat = breakdown.stat();
        List<SheetLine> lines = new ArrayList<>(meaningSheet(stat).lines());
        if (!breakdown.changed() && breakdown.base() == breakdown.current()) {
            return new InfoSheet(lines);
        }
        lines.add(new SheetLine.Divider());
        lines.add(total("Base", wording.read(stat, breakdown.base())));
        breakdown.added().forEach(part -> lines.add(source(part, wording)));
        if (!breakdown.added().isEmpty() && !breakdown.scaled().isEmpty()) {
            lines.add(total("Together", wording.read(stat, breakdown.together())));
        }
        breakdown.scaled().forEach(part -> lines.add(source(part, wording)));
        lines.add(total("Now", wording.read(stat, breakdown.current())));
        return new InfoSheet(lines);
    }

    private static Row total(String label, StatReading reading) {
        return Row.plain(Glyph.DOT, label, reading.number()).withGloss(reading.unit(), "");
    }

    /** One kind of source: its face when it is a term, how many of it, and its change in its stat's unit. */
    private static Row source(StatPart part, StatWording wording) {
        TermText source = part.source();
        String label = source.plain() + (part.stacks() > 1 ? STACKS + part.stacks() : "");
        String change = wording.change(part.stat(), part.modifier());
        List<Term> mentions = source.mentions();
        return mentions.isEmpty() ? Row.plain(Glyph.PIP, label, change)
                : Row.faced(TermLooks.faceOf(mentions.getFirst()), label, change);
    }
}
