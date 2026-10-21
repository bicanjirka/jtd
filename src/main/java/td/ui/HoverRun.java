package td.ui;

import td.term.TermText;
import td.ui.render.SheetLine;

/** What a run of a laid-out sheet is about, stored as its {@link HoverKey} attribute; one object per run. */
sealed interface HoverRun {

    /** A term named in running text: its icon and its title. */
    record MentionRun(TermText.Mention mention) implements HoverRun {
    }

    /** One embedded icon holding a whole row of faces. */
    record IconRowRun(SheetLine.IconRow row, IconRowLayout layout) implements HoverRun {
    }

    /** A row that opens a tooltip. */
    record RowRun(SheetLine.Row row) implements HoverRun {
    }
}
