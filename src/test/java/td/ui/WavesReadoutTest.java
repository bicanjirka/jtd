package td.ui;

import org.junit.jupiter.api.Test;
import td.enemy.Rank;
import td.fixtures.WorldFixtures;
import td.level.LevelOutcome;
import td.ui.RoundStrip.State;
import td.ui.WavesReadout.Heading;
import td.ui.WavesReadout.Skull;
import td.wave.RoundSummary;
import td.wave.Wave;
import td.wave.WaveContent;
import td.wave.WaveProgress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class WavesReadoutTest {

    private static final RoundSummary GRUNTS = new RoundSummary(Rank.GRUNT, false, false);
    private static final RoundSummary LEAKED = new RoundSummary(Rank.GRUNT, false, true);

    private final List<Wave> current = List.of(wave());
    private final List<Wave> next = List.of(wave(), wave());

    private static Wave wave() {
        return new Wave(WorldFixtures.newWorld(), new WaveContent(List.of()), 1L);
    }

    /** As the engine reports it: the round in play's waves once one has started, the next one's while any is left. */
    private WaveProgress progress(int index, List<RoundSummary> rounds) {
        return new WaveProgress(index, rounds, index == 0 ? List.of() : this.current,
                index < rounds.size() ? this.next : List.of());
    }

    @Test
    void beforeTheFirstRoundNowIsReadyAndTheFirstRoundCanBeSent() {
        WavesReadout readout = WavesReadout.of(this.progress(0, Collections.nCopies(4, GRUNTS)), true,
                LevelOutcome.PLAYING);

        assertThat(readout.now()).isEqualTo(new Heading(Optional.empty(), "Ready", "", List.of()));
        assertThat(readout.send()).contains("Send round 1");
        assertThat(readout.next().orElseThrow().title()).isEqualTo("Round 1");
    }

    @Test
    void aRoundInPlayIsHeadedByItsNumberOutOfTheLevelsAndListsItsWaves() {
        WavesReadout readout = WavesReadout.of(this.progress(2, Collections.nCopies(4, GRUNTS)), false,
                LevelOutcome.PLAYING);

        assertThat(readout.now()).isEqualTo(new Heading(Optional.of(new Skull(RoundSkulls.Kind.PLAIN, State.CURRENT)),
                "Round 2", "/4", this.current));
        assertThat(readout.send()).isEmpty();
        assertThat(readout.next()).contains(new Heading(Optional.of(new Skull(RoundSkulls.Kind.PLAIN, State.AHEAD)),
                "Round 3", "/4", this.next));
    }

    @Test
    void onceARoundIsBeatenNowSaysSoWithItsCrackedSkullAndTheNextCanBeSent() {
        WavesReadout readout = WavesReadout.of(this.progress(2, Collections.nCopies(4, GRUNTS)), true,
                LevelOutcome.PLAYING);

        assertThat(readout.now()).isEqualTo(new Heading(Optional.of(new Skull(RoundSkulls.Kind.PLAIN, State.CLEARED)),
                "Round 2", "cleared", List.of()));
        assertThat(readout.send()).contains("Send round 3");
    }

    @Test
    void aRoundAnEnemyEscapedFromReadsLeaked() {
        List<RoundSummary> rounds = new ArrayList<>(Collections.nCopies(4, GRUNTS));
        rounds.set(1, LEAKED);

        WavesReadout readout = WavesReadout.of(this.progress(2, rounds), true, LevelOutcome.PLAYING);

        assertThat(readout.now().suffix()).isEqualTo("leaked");
        assertThat(readout.now().skull().orElseThrow().state()).isEqualTo(State.LEAKED);
    }

    @Test
    void duringTheLastRoundThereIsNoNextRound() {
        WavesReadout readout = WavesReadout.of(this.progress(4, Collections.nCopies(4, GRUNTS)), false,
                LevelOutcome.PLAYING);

        assertThat(readout.next()).isEmpty();
    }

    @Test
    void aLevelThatIsOverOffersNoRoundToSend() {
        WavesReadout readout = WavesReadout.of(this.progress(2, Collections.nCopies(4, GRUNTS)), true,
                LevelOutcome.LOST);

        assertThat(readout.send()).isEmpty();
    }
}
