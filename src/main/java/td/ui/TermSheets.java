package td.ui;

import td.term.Term;
import td.term.TermText;
import td.ui.render.IconFace;
import td.ui.render.InfoSheet;
import td.ui.render.SheetLine;

import java.util.ArrayList;
import java.util.List;

/** The tooltip sheets of terms. Pure. */
final class TermSheets {

    private TermSheets() {
    }

    /** A mention's tooltip: its term's face and title, and what it means here. */
    static InfoSheet of(TermText.Mention mention) {
        return of(mention.term(), mention.meaning(), List.of());
    }

    /** {@code term}'s tooltip: its title line, {@code meaning} as a sentence, then {@code more}. */
    static InfoSheet of(Term term, TermText meaning, List<SheetLine> more) {
        List<SheetLine> lines = new ArrayList<>();
        lines.add(title(term, ""));
        lines.add(SheetLine.Sentence.of(meaning.asSentence()));
        lines.addAll(more);
        return new InfoSheet(lines);
    }

    static SheetLine.IconTitle title(Term term, String value) {
        IconFace face = TermLooks.faceOf(term);
        return new SheetLine.IconTitle(face, term.title(), face.tone(), value);
    }
}
