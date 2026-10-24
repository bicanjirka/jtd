package td.ui;

import td.term.Term;
import td.ui.render.IconFace;
import td.ui.render.Palette;
import td.ui.render.SheetTooltip;
import td.ui.render.TooltipKey;
import td.wave.WaveScriptSuggestions.Candidate;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * A fixed set to choose one from, in a set order, each a row with a title, a hint, a look and maybe a tooltip. An item
 * that can't be taken stays in the list, greyed out, with the reason as its hint.
 */
final class Choices<T> {

    private final List<T> sorted;
    private final List<Candidate> rows;
    /** Built once, so painting a row makes nothing and two rows alike still keep their own look. */
    private final List<RowLook> looks;
    private final List<Boolean> usable;
    private final List<Optional<SheetTooltip>> tooltips;

    Choices(List<T> items, Comparator<T> order, Function<T, String> title, Function<T, String> hint,
            Function<T, RowLook> looks) {
        this(items, order, title, hint, looks, item -> Optional.empty());
    }

    /** {@code unusable} names why an item can't be taken; empty for one that can. */
    Choices(List<T> items, Comparator<T> order, Function<T, String> title, Function<T, String> hint,
            Function<T, RowLook> looks, Function<T, Optional<String>> unusable) {
        this(items, order, title, hint, looks, unusable, item -> Optional.empty());
    }

    private Choices(List<T> items, Comparator<T> order, Function<T, String> title, Function<T, String> hint,
            Function<T, RowLook> looks, Function<T, Optional<String>> unusable,
            Function<T, Optional<SheetTooltip>> tooltips) {
        this.sorted = items.stream().sorted(order).toList();
        this.rows = this.sorted.stream()
                .map(item -> Candidate.token(title.apply(item), unusable.apply(item).orElseGet(() -> hint.apply(item))))
                .toList();
        this.looks = this.sorted.stream()
                .map(item -> unusable.apply(item).isPresent() ? looks.apply(item).dimmed() : looks.apply(item))
                .toList();
        this.usable = this.sorted.stream().map(item -> unusable.apply(item).isEmpty()).toList();
        this.tooltips = this.sorted.stream().map(tooltips).toList();
    }

    /**
     * Terms grouped by type and then by title, each shown with its icon and hinted with its type, written in the
     * type's frame, its tooltip saying what it does; {@code byType} orders the groups.
     */
    static <T extends Term> Choices<T> ofTerms(List<T> terms, Comparator<T> byType, Function<T, String> typeLabel,
            Function<T, Palette> typeFrame, Function<T, IconFace> faces) {
        return ofTerms(terms, byType, typeLabel, typeFrame, faces, term -> Optional.empty());
    }

    /** The same, a term that can't be taken last in its group, hinted with why. */
    static <T extends Term> Choices<T> ofTerms(List<T> terms, Comparator<T> byType, Function<T, String> typeLabel,
            Function<T, Palette> typeFrame, Function<T, IconFace> faces, Function<T, Optional<String>> unusable) {
        Comparator<T> usableFirst = Comparator.comparing(term -> unusable.apply(term).isPresent());
        return new Choices<>(terms, byType.thenComparing(usableFirst).thenComparing(Term::title, String.CASE_INSENSITIVE_ORDER),
                Term::title, typeLabel, term -> {
                    IconFace face = faces.apply(term);
                    return new RowLook(new PaintedIcon(TermIconPainter.MENTION, TermIconPainter.MENTION, g2 -> {
                        g2.translate(-TermIconPainter.MENTION / 2.0, -TermIconPainter.MENTION / 2.0);
                        TermIconPainter.paint(g2, face, TermIconPainter.MENTION);
                    }), TermIconPainter.ink(face.tone()), TermIconPainter.ink(typeFrame.apply(term)));
                }, unusable, term -> Optional.of(new SheetTooltip(TooltipKey.ofTerm("choice", term),
                        TermSheets.of(faces.apply(term), term, term.describe(), List.of()))));
    }

    List<Candidate> rows() {
        return this.rows;
    }

    /** Where {@code item} sits among the rows; -1 for one not offered. */
    int indexOf(T item) {
        return this.sorted.indexOf(item);
    }

    T itemAt(int index) {
        return this.sorted.get(index);
    }

    Candidate rowOf(T item) {
        return this.rows.get(this.indexOf(item));
    }

    RowLook lookOf(T item) {
        return this.looks.get(this.indexOf(item));
    }

    RowLook lookAt(int index) {
        return this.looks.get(index);
    }

    /** What hovering the row at {@code index} shows, whether or not it can be taken. */
    Optional<SheetTooltip> tooltipAt(int index) {
        return this.tooltips.get(index);
    }

    /** Whether the row at {@code index} can be taken. */
    boolean usableAt(int index) {
        return this.usable.get(index);
    }
}
