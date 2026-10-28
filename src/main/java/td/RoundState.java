package td;

import td.level.LevelOutcome;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Where a level's rounds stand: the waves started so far, the phase, the rounds an enemy escaped,
 * and the leak count when the running round started. Every change is a new value, so a reader on
 * another thread sees one coherent state.
 *
 * @param wave         waves started so far; the next to start is {@code wave + 1}
 * @param leaked       0-based rounds an enemy escaped
 * @param leaksAtStart the roster's leak count when the running round started
 */
record RoundState(int wave, Phase phase, Set<Integer> leaked, long leaksAtStart) {

    enum Phase {
        /** Between waves, the board clear: the next wave may start. */
        READY,
        /** A wave is out. */
        RUNNING,
        WON,
        LOST
    }

    RoundState {
        leaked = Set.copyOf(leaked);
    }

    /** Nothing played yet, with wave {@code wave + 1} next. */
    static RoundState before(int wave) {
        return new RoundState(wave, Phase.READY, Set.of(), 0L);
    }

    boolean isWaveReady() {
        return this.phase == Phase.READY;
    }

    LevelOutcome outcome() {
        return switch (this.phase) {
            case WON -> LevelOutcome.WON;
            case LOST -> LevelOutcome.LOST;
            case READY, RUNNING -> LevelOutcome.PLAYING;
        };
    }

    boolean canBegin(int waveCount) {
        return this.phase == Phase.READY && this.wave < waveCount;
    }

    /** The next wave is out; {@code leaks} is the roster's count now. */
    RoundState begun(long leaks) {
        return new RoundState(this.wave + 1, Phase.RUNNING, this.leaked, leaks);
    }

    /**
     * The round at the end of a tick that left {@code alive} enemies on the board. Loss comes first:
     * a last enemy leaking the last life loses.
     */
    RoundState settled(int alive, long leaks, boolean livesGone, int waveCount) {
        if (this.outcome().isOver()) {
            return this;
        }
        if (livesGone) {
            return this.noted(leaks).in(Phase.LOST);
        }
        return switch (this.phase) {
            case RUNNING -> alive > 0 ? this : this.noted(leaks).in(this.wave < waveCount ? Phase.READY : Phase.WON);
            case READY, WON, LOST -> this;
        };
    }

    /** Debug: the board was cleared, so the round in play ends with nothing more to settle. */
    RoundState skipped(long leaks) {
        return this.noted(leaks).in(Phase.READY);
    }

    RoundState won() {
        return this.in(Phase.WON);
    }

    /** Debug: wave {@code next} (1-based) starts next, and the rounds from it on have not been played. */
    RoundState jumpedTo(int next) {
        Set<Integer> earlier = this.leaked.stream().filter(round -> round < next - 1).collect(Collectors.toSet());
        return new RoundState(next - 1, Phase.READY, earlier, this.leaksAtStart);
    }

    /** Marks the running round as one an enemy escaped, if one has since it started. */
    private RoundState noted(long leaks) {
        if (this.phase != Phase.RUNNING || leaks <= this.leaksAtStart) {
            return this;
        }
        Set<Integer> more = new HashSet<>(this.leaked);
        more.add(this.wave - 1);
        return new RoundState(this.wave, this.phase, more, this.leaksAtStart);
    }

    private RoundState in(Phase next) {
        return new RoundState(this.wave, next, this.leaked, this.leaksAtStart);
    }
}
