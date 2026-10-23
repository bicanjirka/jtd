package td.ui;

import td.term.Term;
import td.ui.render.IconFace;
import td.ui.render.Palette;
import td.wave.WaveScriptSuggestions.Candidate;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** A fixed set to choose one from, in a set order, each a row with a title, a hint and a look. */
final class Choices<T> {

    private final List<T> sorted;
    private final List<Candidate> rows;
    private final Function<T, RowLook> looks;

    Choices(List<T> items, Comparator<T> order, Function<T, String> title, Function<T, String> hint,
            Function<T, RowLook> looks) {
        this.sorted = items.stream().sorted(order).toList();
        this.rows = this.sorted.stream().map(item -> Candidate.token(title.apply(item), hint.apply(item))).toList();
        this.looks = looks;
    }

    /**
     * Terms grouped by type and then by title, each shown with its icon and hinted with its type, written in the
     * type's frame; {@code byType} orders the groups.
     */
    static <T extends Term> Choices<T> ofTerms(List<T> terms, Comparator<T> byType, Function<T, String> typeLabel,
            Function<T, Palette> typeFrame, Function<T, IconFace> faces) {
        return new Choices<>(terms, byType.thenComparing(Term::title, String.CASE_INSENSITIVE_ORDER), Term::title,
                typeLabel, term -> {
                    IconFace face = faces.apply(term);
                    return new RowLook(new PaintedIcon(TermIconPainter.MENTION, TermIconPainter.MENTION, g2 -> {
                        g2.translate(-TermIconPainter.MENTION / 2.0, -TermIconPainter.MENTION / 2.0);
                        TermIconPainter.paint(g2, face, TermIconPainter.MENTION);
                    }), TermIconPainter.ink(face.tone()), TermIconPainter.ink(typeFrame.apply(term)));
                });
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
        return this.looks.apply(item);
    }

    Optional<RowLook> lookOfRow(Candidate row) {
        int index = this.rows.indexOf(row);
        return index < 0 ? Optional.empty() : Optional.of(this.looks.apply(this.sorted.get(index)));
    }
}
