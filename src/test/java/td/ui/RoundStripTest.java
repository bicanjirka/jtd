package td.ui;

import org.junit.jupiter.api.Test;
import td.enemy.Rank;
import td.level.LevelOutcome;
import td.ui.RoundStrip.State;
import td.wave.RoundSummary;
import td.wave.WaveProgress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoundStripTest {

    private static WaveProgress progress(int index, List<RoundSummary> rounds) {
        return new WaveProgress(index, rounds, List.of(), List.of());
    }

    private static List<RoundSummary> grunts(int count) {
        return Collections.nCopies(count, new RoundSummary(Rank.GRUNT, false));
    }

    @Test
    void beforeTheFirstRoundTheFirstIsCurrentAndTheRestAreAhead() {
        RoundStrip strip = RoundStrip.of(progress(0, grunts(3)), true, LevelOutcome.PLAYING);

        assertThat(strip.states()).containsExactly(State.CURRENT, State.AHEAD, State.AHEAD);
    }

    @Test
    void theRoundInPlayIsCurrentAndTheOnesBeforeItAreOver() {
        List<RoundSummary> rounds = new ArrayList<>(grunts(4));
        rounds.set(1, new RoundSummary(Rank.GRUNT, true));

        RoundStrip strip = RoundStrip.of(progress(3, rounds), false, LevelOutcome.PLAYING);

        assertThat(strip.states()).containsExactly(State.CLEARED, State.LEAKED, State.CURRENT, State.AHEAD);
    }

    @Test
    void betweenRoundsTheNextToSendIsCurrent() {
        RoundStrip strip = RoundStrip.of(progress(2, grunts(4)), true, LevelOutcome.PLAYING);

        assertThat(strip.states()).containsExactly(State.CLEARED, State.CLEARED, State.CURRENT, State.AHEAD);
    }

    @Test
    void theRoundALevelIsLostOnLeakedAndNoneIsCurrent() {
        RoundStrip strip = RoundStrip.of(progress(2, grunts(4)), false, LevelOutcome.LOST);

        assertThat(strip.states()).containsExactly(State.CLEARED, State.LEAKED, State.AHEAD, State.AHEAD);
    }

    @Test
    void aWonLevelHasEveryRoundOver() {
        RoundStrip strip = RoundStrip.of(progress(3, grunts(3)), false, LevelOutcome.WON);

        assertThat(strip.states()).containsOnly(State.CLEARED);
    }

    @Test
    void aShortLevelShowsEveryRound() {
        RoundStrip strip = RoundStrip.of(progress(4, grunts(8)), false, LevelOutcome.PLAYING);

        assertThat(strip.window(10)).isEqualTo(new RoundStrip.Window(0, 8, 8));
    }

    @Test
    void theWindowSlidesToKeepOneBeatenRoundBeforeTheCurrentOne() {
        RoundStrip strip = RoundStrip.of(progress(8, grunts(24)), false, LevelOutcome.PLAYING);

        RoundStrip.Window window = strip.window(10);

        assertThat(window.start()).isEqualTo(6);
        assertThat(window.hiddenAfter()).isEqualTo(8);
    }

    @Test
    void theWindowStopsAtTheLastRounds() {
        RoundStrip strip = RoundStrip.of(progress(24, grunts(24)), false, LevelOutcome.WON);

        assertThat(strip.window(10)).isEqualTo(new RoundStrip.Window(14, 24, 24));
    }

    @Test
    void theHiddenRoundsNameTheHighestRankAmongThem() {
        List<RoundSummary> rounds = new ArrayList<>(grunts(12));
        rounds.set(11, new RoundSummary(Rank.BOSS, false));
        RoundStrip strip = RoundStrip.of(progress(0, rounds), true, LevelOutcome.PLAYING);

        assertThat(strip.highestRankFrom(strip.window(10).end())).isEqualTo(Rank.BOSS);
    }

    @Test
    void everyRankAboveSoldierHasItsOwnSkullAndAnOverRoundIsCracked() {
        assertThat(RoundSkulls.pictogram(Rank.GRUNT, State.AHEAD)).isEqualTo(RoundSkulls.pictogram(Rank.SOLDIER, State.AHEAD));
        assertThat(List.of(Rank.SOLDIER, Rank.VETERAN, Rank.ELITE, Rank.BOSS))
                .extracting(rank -> RoundSkulls.pictogram(rank, State.AHEAD)).doesNotHaveDuplicates();
        for (Rank rank : Rank.values()) {
            for (State state : State.values()) {
                assertThat(PictogramCatalogue.of(RoundSkulls.pictogram(rank, state))).isPresent();
            }
        }
        assertThat(RoundSkulls.pictogram(Rank.BOSS, State.LEAKED)).isNotEqualTo(RoundSkulls.pictogram(Rank.BOSS, State.CURRENT));
    }
}
