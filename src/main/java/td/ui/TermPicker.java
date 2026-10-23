package td.ui;

import td.term.Term;
import td.ui.render.IconFace;
import td.wave.WaveScriptSuggestions.Candidate;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** A set of terms to choose one from, grouped by type and then by title, each a row hinted with its type. */
final class TermPicker<T extends Term> {

    private final List<T> sorted;
    private final Function<T, String> typeLabel;
    private final Function<T, IconFace> faces;

    /** {@code byType} orders the groups; {@code typeLabel} names a term's group, as its row's hint. */
    TermPicker(List<T> terms, Comparator<T> byType, Function<T, String> typeLabel, Function<T, IconFace> faces) {
        this.sorted = terms.stream()
                .sorted(byType.thenComparing(Term::title, String.CASE_INSENSITIVE_ORDER))
                .toList();
        this.typeLabel = typeLabel;
        this.faces = faces;
    }

    List<Candidate> rows() {
        return this.sorted.stream().map(term -> Candidate.token(term.title(), this.typeLabel.apply(term))).toList();
    }

    /** Where {@code term} sits among the rows; -1 for a term not offered. */
    int indexOf(T term) {
        return this.sorted.indexOf(term);
    }

    /** The term a row names. */
    T termAt(int index) {
        return this.sorted.get(index);
    }

    IconFace faceOf(T term) {
        return this.faces.apply(term);
    }

    /** The icon of the term a row names. */
    Optional<IconFace> faceOfRow(Candidate row) {
        return this.sorted.stream().filter(term -> term.title().equals(row.text())).findFirst().map(this.faces);
    }
}
