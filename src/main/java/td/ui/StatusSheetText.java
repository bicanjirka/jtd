package td.ui;

import td.economy.StatusTerm;
import td.ui.render.IconFace;
import td.ui.render.InfoSheet;
import td.ui.render.SheetIcon;
import td.ui.render.SheetLine;
import td.ui.render.SheetLine.Glyph;
import td.ui.render.SheetLine.Row;
import td.ui.render.TooltipKey;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/** The tooltip of a status bar icon: its face and value, what it means, and where the readout knows more. Pure. */
final class StatusSheetText {

    private StatusSheetText() {
    }

    static InfoSheet of(StatusTerm term, StatusReadout readout, RoundStrip rounds) {
        List<SheetLine> more = new ArrayList<>();
        if (term == StatusTerm.WAVE) {
            more.add(Row.plain(Glyph.DOT, "Rounds started", readout.round() + " / " + readout.rounds()));
            more.add(waveState(readout));
            if (rounds.count() > 0) {
                more.add(new SheetLine.IconRow("Rounds", IntStream.range(0, rounds.count())
                        .mapToObj(round -> roundIcon(rounds, round))
                        .toList()));
            }
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

    private static SheetIcon roundIcon(RoundStrip rounds, int round) {
        IconFace face = RoundSkulls.face(rounds, round);
        InfoSheet sheet = new InfoSheet(List.of(
                new SheetLine.IconTitle(face.withBelow(""), "Round " + (round + 1), face.tone(),
                        RoundSkulls.stateName(rounds.stateAt(round))),
                Row.plain(Glyph.SKULL, "Strongest rank", RoundSkulls.rankName(rounds.rankAt(round)))));
        return new SheetIcon(face, TooltipKey.of("round", Integer.toString(round)), sheet);
    }
}
