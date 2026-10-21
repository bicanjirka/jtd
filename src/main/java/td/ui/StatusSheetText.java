package td.ui;

import td.economy.StatusTerm;
import td.ui.render.InfoSheet;
import td.ui.render.SheetLine;
import td.ui.render.SheetLine.Glyph;
import td.ui.render.SheetLine.Row;

import java.util.ArrayList;
import java.util.List;

/** The tooltip of a status bar icon: its face and value, what it means, and where the readout knows more. Pure. */
final class StatusSheetText {

    private StatusSheetText() {
    }

    static InfoSheet of(StatusTerm term, StatusReadout readout) {
        List<SheetLine> more = new ArrayList<>();
        if (term == StatusTerm.WAVE) {
            more.add(Row.plain(Glyph.DOT, "Rounds started", readout.round() + " / " + readout.rounds()));
            more.add(waveState(readout));
        }
        List<SheetLine> lines = new ArrayList<>(TermSheets.of(term, term.describe(), more).lines());
        lines.set(0, TermSheets.title(term, readout.value(term)));
        return new InfoSheet(lines);
    }

    private static Row waveState(StatusReadout readout) {
        if (readout.levelOver()) {
            return Row.plain(Glyph.DOT, "Level", "over");
        }
        return readout.waveReady() ? Row.plain(Glyph.DOT, "Next round", "ready") : Row.plain(Glyph.DOT, "Round", "in play");
    }
}
