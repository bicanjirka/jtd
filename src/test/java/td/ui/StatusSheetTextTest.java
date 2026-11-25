package td.ui;

import org.junit.jupiter.api.Test;
import td.economy.StatusTerm;
import td.enemy.Rank;
import td.level.LevelOutcome;
import td.term.TermText;
import td.ui.render.InfoSheet;
import td.ui.render.SheetLine;
import td.ui.render.SheetLine.Glyph;
import td.ui.render.SheetLine.Row;
import td.wave.RoundPay;
import td.wave.RoundSummary;
import td.wave.WaveProgress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StatusSheetTextTest {

    private static final StatusReadout PLAYING = new StatusReadout(7, 18, false, false, 5, 1240, 4964);

    @Test
    void theLivesTooltipOpensWithInfinityAndSaysWhatIsHeldWhileLivesAreUnlimited() {
        InfoSheet sheet = StatusSheetText.of(StatusTerm.LIVES, PLAYING.withCheats(new StatusCheats(true, false)), RoundStrip.empty());

        assertThat(((SheetLine.IconTitle) sheet.lines().getFirst()).value()).isEqualTo("∞");
        assertThat(sheet.lines()).contains(Row.plain(Glyph.DOT, "Infinite lives", "5 held"));
    }

    @Test
    void theCashTooltipSaysSoDuringFreeBuild() {
        InfoSheet sheet = StatusSheetText.of(StatusTerm.CASH, PLAYING.withCheats(new StatusCheats(false, true)), RoundStrip.empty());

        assertThat(sheet.lines()).contains(Row.plain(Glyph.DOT, "Free build", "on"));
    }

    @Test
    void theWaveTooltipSaysWhetherARoundIsInPlay() {
        InfoSheet sheet = StatusSheetText.of(StatusTerm.WAVE, PLAYING, RoundStrip.empty());

        assertThat(sheet.lines()).contains(Row.plain(Glyph.DOT, "Round", "in play"));
    }

    /** The phrases of round 2's tooltip, on a level at round {@code index}, whose rounds pay as {@code second} does. */
    private static List<String> payPhrases(RoundSummary second, int index, boolean waveReady) {
        List<RoundSummary> rounds = new ArrayList<>(Collections.nCopies(4, new RoundSummary(Rank.GRUNT, false, false)));
        rounds.set(1, second);
        RoundStrip strip = RoundStrip.of(new WaveProgress(index, rounds, List.of(), List.of()), waveReady,
                LevelOutcome.PLAYING);
        SheetLine.IconRow row = (SheetLine.IconRow) StatusSheetText.of(StatusTerm.WAVE, PLAYING, strip).lines().getLast();
        return row.icons().get(1).tooltip().lines().stream()
                .filter(line -> line instanceof Row paying && paying.face().isPresent())
                .map(line -> ((Row) line).label())
                .toList();
    }

    @Test
    void aRoundToComeSaysItsBountyAboveWhatItOffersForNoLeaks() {
        RoundSummary round = new RoundSummary(Rank.GRUNT, false, false).withPay(new RoundPay(86, 5, 0));

        assertThat(payPhrases(round, 0, true)).containsExactly("+86 bounty", "+5 if no leaks");
    }

    @Test
    void aClearedRoundSaysWhatItPaid() {
        RoundSummary round = new RoundSummary(Rank.GRUNT, false, false).withPay(new RoundPay(86, 5, 5));

        assertThat(payPhrases(round, 3, true)).containsExactly("+86 bounty", "+5 paid");
    }

    @Test
    void aRoundAnEnemyEscapedSaysItsBonusIsLost() {
        RoundSummary round = new RoundSummary(Rank.GRUNT, false, true).withPay(new RoundPay(86, 5, 0));

        assertThat(payPhrases(round, 3, true)).containsExactly("+86 bounty", "+5 lost");
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
    void theCashTextQuotesTheSellRefundFromItsRule() {
        assertThat(StatusTerm.CASH.describe().plain()).contains("75%");
    }

    @Test
    void everyTermTextStartsLowerCaseWithNoFullStop() {
        for (StatusTerm term : StatusTerm.values()) {
            TermText text = term.describe();

            assertThat(text.plain()).matches("[a-z].*[^.]");
        }
    }
}
