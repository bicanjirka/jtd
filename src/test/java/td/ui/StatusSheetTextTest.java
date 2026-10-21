package td.ui;

import org.junit.jupiter.api.Test;
import td.economy.StatusTerm;
import td.enemy.Rank;
import td.level.LevelOutcome;
import td.term.TermText;
import td.ui.render.InfoSheet;
import td.ui.render.Palette;
import td.ui.render.SheetLine;
import td.ui.render.SheetLine.Glyph;
import td.ui.render.SheetLine.Row;
import td.wave.RoundSummary;
import td.wave.WaveProgress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StatusSheetTextTest {

    private static final StatusReadout PLAYING = new StatusReadout(7, 18, false, false, 5, 1240, 4964);

    @Test
    void theBarWritesEachValueBesideItsIcon() {
        assertThat(PLAYING.value(StatusTerm.WAVE)).isEqualTo("7/18");
        assertThat(PLAYING.value(StatusTerm.LIVES)).isEqualTo("5");
        assertThat(PLAYING.value(StatusTerm.SCORE)).isEqualTo("1240");
        assertThat(PLAYING.value(StatusTerm.CASH)).isEqualTo("4964");
    }

    @Test
    void everyTooltipOpensWithTheFaceAndTitleAndTheValueOnTheRight() {
        for (StatusTerm term : StatusTerm.values()) {
            SheetLine.IconTitle title = (SheetLine.IconTitle) StatusSheetText.of(term, PLAYING, RoundStrip.empty()).lines().getFirst();

            assertThat(title.name()).isEqualTo(term.title());
            assertThat(title.value()).isEqualTo(PLAYING.value(term));
            assertThat(title.face()).isEqualTo(TermLooks.faceOf(term));
        }
    }

    @Test
    void everyTooltipSaysWhatItsTermMeansAsOneSentence() {
        for (StatusTerm term : StatusTerm.values()) {
            InfoSheet sheet = StatusSheetText.of(term, PLAYING, RoundStrip.empty());

            assertThat(sheet.lines().get(1)).isEqualTo(SheetLine.Sentence.of(term.describe().asSentence()));
        }
    }

    @Test
    void onlyTheWaveTooltipHasLiveRows() {
        assertThat(StatusSheetText.of(StatusTerm.LIVES, PLAYING, RoundStrip.empty()).lines()).hasSize(2);
        assertThat(StatusSheetText.of(StatusTerm.SCORE, PLAYING, RoundStrip.empty()).lines()).hasSize(2);
        assertThat(StatusSheetText.of(StatusTerm.CASH, PLAYING, RoundStrip.empty()).lines()).hasSize(2);
        assertThat(StatusSheetText.of(StatusTerm.WAVE, PLAYING, RoundStrip.empty()).lines()).hasSize(4);
    }

    @Test
    void theWaveTooltipCountsRoundsStartedAndSaysWhetherARoundIsInPlay() {
        InfoSheet sheet = StatusSheetText.of(StatusTerm.WAVE, PLAYING, RoundStrip.empty());

        assertThat(sheet.lines()).contains(Row.plain(Glyph.DOT, "Rounds started", "7 / 18"),
                Row.plain(Glyph.DOT, "Round", "in play"));
    }

    @Test
    void theWaveTooltipSaysTheNextRoundIsReadyBetweenRounds() {
        StatusReadout between = new StatusReadout(7, 18, true, false, 5, 0, 0);

        assertThat(StatusSheetText.of(StatusTerm.WAVE, between, RoundStrip.empty()).lines()).contains(Row.plain(Glyph.DOT, "Next round", "ready"));
    }

    @Test
    void theWaveTooltipSaysTheLevelIsOverOnceItIs() {
        StatusReadout over = new StatusReadout(18, 18, true, true, 0, 0, 0);

        assertThat(StatusSheetText.of(StatusTerm.WAVE, over, RoundStrip.empty()).lines()).contains(Row.plain(Glyph.DOT, "Level", "over"));
    }

    @Test
    void theWaveTooltipShowsEveryRoundsSkullNumberedAndEachOpensItsOwnTooltip() {
        List<RoundSummary> rounds = new ArrayList<>(Collections.nCopies(18, new RoundSummary(Rank.GRUNT, false, false)));
        rounds.set(2, new RoundSummary(Rank.ELITE, false, true));
        RoundStrip strip = RoundStrip.of(new WaveProgress(7, rounds, List.of(), List.of()), false, LevelOutcome.PLAYING);

        SheetLine.IconRow row = (SheetLine.IconRow) StatusSheetText.of(StatusTerm.WAVE, PLAYING, strip).lines().getLast();

        assertThat(row.icons()).hasSize(18);
        assertThat(row.icons()).extracting(icon -> icon.face().below()).startsWith("1", "2", "3");
        assertThat(row.icons().get(2).tooltip().lines()).contains(
                Row.plain(Glyph.SKULL, "Strongest", "Elite"));
        assertThat(((SheetLine.IconTitle) row.icons().get(2).tooltip().lines().getFirst()).value())
                .isEqualTo("enemies escaped");
        assertThat(row.icons().get(6).face().frame()).isEqualTo(Palette.ROUND_CURRENT);
    }

    @Test
    void theCashTextQuotesTheSellRefundFromItsRule() {
        assertThat(StatusTerm.CASH.describe().plain()).contains("75%");
    }

    @Test
    void fewLivesAreHeartsAndTooManyOrNoneAreANumber() {
        assertThat(new StatusReadout(1, 18, false, false, 1, 0, 0).hearts()).hasValue(1);
        assertThat(new StatusReadout(1, 18, false, false, 10, 0, 0).hearts()).hasValue(10);
        assertThat(new StatusReadout(1, 18, false, false, 11, 0, 0).hearts()).isEmpty();
        assertThat(new StatusReadout(1, 18, false, true, 0, 0, 0).hearts()).isEmpty();
    }

    @Test
    void everyTermTextStartsLowerCaseWithNoFullStop() {
        for (StatusTerm term : StatusTerm.values()) {
            TermText text = term.describe();

            assertThat(text.plain()).matches("[a-z].*[^.]");
        }
    }
}
