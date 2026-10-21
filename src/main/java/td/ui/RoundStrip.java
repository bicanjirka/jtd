package td.ui;

import td.enemy.Rank;
import td.level.LevelOutcome;
import td.wave.RoundSummary;
import td.wave.WaveProgress;

import java.util.ArrayList;
import java.util.List;

/**
 * The level's rounds as the wave readout draws them: each round's skull and how it stands. Pure.
 *
 * @param rounds every round, in order
 * @param states each round's standing, parallel to {@code rounds}
 */
record RoundStrip(List<RoundSummary> rounds, List<RoundStrip.State> states) {

    /** Where a round stands. */
    enum State {
        /** Still to come. */
        AHEAD,
        /** In play, or the next to send while none is. */
        CURRENT,
        /** Beaten with nothing escaping. */
        CLEARED,
        /** Over, with an enemy that escaped. */
        LEAKED
    }

    /** Which rounds a strip of limited room shows: {@code [start, end)}, the rest hidden. */
    record Window(int start, int end, int count) {

        int hiddenAfter() {
            return this.count - this.end;
        }
    }

    RoundStrip {
        rounds = List.copyOf(rounds);
        states = List.copyOf(states);
    }

    static RoundStrip empty() {
        return new RoundStrip(List.of(), List.of());
    }

    static RoundStrip of(WaveProgress progress, boolean waveReady, LevelOutcome outcome) {
        int index = progress.index();
        // Between rounds, or once won, every round started is over; otherwise the last one started is not.
        int over = waveReady || outcome == LevelOutcome.WON ? index : index - 1;
        int current = waveReady ? index : index - 1;
        List<State> states = new ArrayList<>();
        for (int round = 0; round < progress.count(); round++) {
            boolean breached = outcome == LevelOutcome.LOST && round == index - 1;
            if (breached || round < over && progress.rounds().get(round).leaked()) {
                states.add(State.LEAKED);
            } else if (round < over) {
                states.add(State.CLEARED);
            } else if (outcome == LevelOutcome.PLAYING && round == current) {
                states.add(State.CURRENT);
            } else {
                states.add(State.AHEAD);
            }
        }
        return new RoundStrip(progress.rounds(), states);
    }

    int count() {
        return this.rounds.size();
    }

    Rank rankAt(int round) {
        return this.rounds.get(round).highestRank();
    }

    State stateAt(int round) {
        return this.states.get(round);
    }

    /** The round the player faces: the current one, else the last one over, else the first. */
    int focus() {
        int current = this.states.indexOf(State.CURRENT);
        if (current >= 0) {
            return current;
        }
        int ahead = this.states.indexOf(State.AHEAD);
        return ahead > 0 ? ahead - 1 : ahead == 0 ? 0 : Math.max(0, this.count() - 1);
    }

    /** At most {@code slots} rounds, one beaten round kept before the focus so the window slides with play. */
    Window window(int slots) {
        if (this.count() <= slots) {
            return new Window(0, this.count(), this.count());
        }
        int start = Math.clamp(this.focus() - 1L, 0, this.count() - slots);
        return new Window(start, start + slots, this.count());
    }

    /** The highest rank among rounds {@code from} on; {@link Rank#GRUNT} past the last. */
    Rank highestRankFrom(int from) {
        return this.rounds.subList(Math.min(from, this.count()), this.count()).stream()
                .map(RoundSummary::highestRank)
                .max(Rank::compareTo)
                .orElse(Rank.GRUNT);
    }
}
