package td.stat;

import td.term.TermNumbers;

import java.util.ArrayList;
import java.util.List;

/**
 * How every stat's value and every change to one read, on rows and in prose alike: a value in its unit with its
 * meaning, a change by its kind ({@code "+0.5 cells"}, {@code "+25%"}, {@code "x0.7"}, {@code "= 0"},
 * {@code "at most 75%"}).
 *
 * @param cellPixels the board's cell size, for a stat stored in pixels
 */
public record StatWording(float cellPixels) {

    public StatReading read(StatTerm stat, float value) {
        StatUnit unit = stat.unit();
        return new StatReading(unit.number(unit.shown(value, this.cellPixels)), unit.word(), stat.inline(value));
    }

    /** {@code value} with its unit, as prose quotes it: {@code "4.5 cells"}. */
    public String value(StatTerm stat, float value) {
        return this.read(stat, value).text();
    }

    /** What {@code modifier} does to {@code stat}, each part by its kind, joined with ", ". */
    public String change(StatTerm stat, StatModifier modifier) {
        StatUnit unit = stat.unit();
        List<String> parts = new ArrayList<>();
        if (modifier.hasSetValue()) {
            parts.add("= " + this.value(stat, modifier.setTo()));
        }
        if (modifier.flat() != 0f) {
            String number = unit.signed(unit.shown(modifier.flat(), this.cellPixels));
            parts.add(unit.word().isEmpty() ? number : number + " " + unit.word());
        }
        if (modifier.percentAdd() != 0f) {
            parts.add(TermNumbers.signedPercent(modifier.percentAdd()));
        }
        if (modifier.multiply() != 1f) {
            parts.add(TermNumbers.times(modifier.multiply()));
        }
        if (modifier.hasCap()) {
            parts.add("at most " + TermNumbers.percent(modifier.capShareOfBase()));
        }
        return String.join(", ", parts);
    }
}
