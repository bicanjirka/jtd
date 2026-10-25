package td.ui;

import td.level.LevelOutcome;
import td.wave.Wave;
import td.wave.WaveProgress;

import java.util.List;
import java.util.Optional;

/**
 * What the Waves panel says, read from the same snapshot as the header's skulls so the two never disagree. Pure.
 *
 * @param now  the round in play, how the last one ended, or that none has started
 * @param send the prompt to send the next round, while one can be sent
 * @param next the round after the one in play; empty during the last
 */
record WavesReadout(Heading now, Optional<String> send, Optional<Heading> next) {

    /** A round's skull, as the header draws it. */
    record Skull(RoundSkulls.Kind kind, RoundStrip.State state) {
    }

    /**
     * One heading line and the waves under it.
     *
     * @param skull  the round's skull; empty before any round
     * @param title  the round's number, or what the panel waits for
     * @param suffix after the title: the level's round count, or how the round ended
     * @param waves  one per path; empty for a round that is over
     */
    record Heading(Optional<Skull> skull, String title, String suffix, List<Wave> waves) {

        Heading {
            waves = List.copyOf(waves);
        }
    }

    static WavesReadout of(WaveProgress progress, boolean waveReady, LevelOutcome outcome) {
        RoundStrip strip = RoundStrip.of(progress, waveReady, outcome);
        Optional<String> send = waveReady && progress.hasNextWave() && !outcome.isOver()
                ? Optional.of("Send round " + progress.nextNumber())
                : Optional.empty();
        Optional<Heading> next = progress.next().isEmpty()
                ? Optional.empty()
                : Optional.of(round(strip, progress.index(), progress.next()));
        return new WavesReadout(now(strip, progress), send, next);
    }

    private static Heading now(RoundStrip strip, WaveProgress progress) {
        int round = progress.index() - 1;
        if (round < 0) {
            return new Heading(Optional.empty(), "Ready", "", List.of());
        }
        RoundStrip.State state = strip.stateAt(round);
        if (state.isOver()) {
            String ended = state == RoundStrip.State.LEAKED ? "leaked" : "cleared";
            return new Heading(Optional.of(new Skull(strip.kindAt(round), state)), title(round), ended, List.of());
        }
        return round(strip, round, progress.current());
    }

    private static Heading round(RoundStrip strip, int round, List<Wave> waves) {
        return new Heading(Optional.of(new Skull(strip.kindAt(round), strip.stateAt(round))), title(round),
                "/" + strip.count(), waves);
    }

    private static String title(int round) {
        return "Round " + (round + 1);
    }
}
