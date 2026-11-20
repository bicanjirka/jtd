package td.ui;

import td.term.TermText;
import td.ui.render.SheetLine;
import td.ui.render.SheetTooltip;

/** What a run of a laid-out sheet is about, stored as its {@link HoverKey} attribute; one object per run. */
sealed interface HoverRun {

    /** A term named in running text: its icon and its title. */
    record MentionRun(TermText.Mention mention) implements HoverRun {
    }

    /** One embedded icon holding a whole row of faces. */
    record IconRowRun(SheetLine.IconRow row, IconRowLayout layout) implements HoverRun {
    }

    /** One embedded icon holding a magazine's slots, each opening its own tooltip. */
    record SlotRowRun(SheetLine.MagazineRow row, SlotRowLayout layout) implements HoverRun {
    }

    /** A whole line that opens a tooltip: a row, an ability's bar. */
    record RowRun(SheetTooltip tooltip) implements HoverRun {
    }
}
