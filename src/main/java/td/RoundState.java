package td;

import td.level.LevelOutcome;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
 * @param bonusesPaid  the clear bonus each 0-based round paid; a round that paid none is absent
 */
record RoundState(int wave, Phase phase, Set<Integer> leaked, long leaksAtStart, Map<Integer, Integer> bonusesPaid) {

    enum Phase {
        /** Between waves, the board clear: the next wave may start. */
        READY,
        /** A wave is out. */
        RUNNING,
        /** Between waves, with enemies no wave sent on the board: the next wave waits until they are gone. */
        OCCUPIED,
        WON,
        LOST
    }

    RoundState {
        leaked = Set.copyOf(leaked);
        bonusesPaid = Map.copyOf(bonusesPaid);
    }

    /** Between rounds with wave {@code wave + 1} next, the rounds before it played as {@code leaked} and {@code paid} say. */
    static RoundState between(int wave, Set<Integer> leaked, Map<Integer, Integer> paid) {
        return new RoundState(wave, Phase.READY, leaked, 0L, paid);
    }

    /** Nothing played yet, with wave {@code wave + 1} next. */
    static RoundState before(int wave) {
        return between(wave, Set.of(), Map.of());
    }

    boolean isWaveReady() {
        return this.phase == Phase.READY;
    }

    LevelOutcome outcome() {
        return switch (this.phase) {
            case WON -> LevelOutcome.WON;
            case LOST -> LevelOutcome.LOST;
            case READY, RUNNING, OCCUPIED -> LevelOutcome.PLAYING;
        };
    }

    boolean canBegin(int waveCount) {
        return this.phase == Phase.READY && this.wave < waveCount;
    }

    /** The next wave is out; {@code leaks} is the roster's count now. */
    RoundState begun(long leaks) {
        return new RoundState(this.wave + 1, Phase.RUNNING, this.leaked, leaks, this.bonusesPaid);
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
            case READY -> this.joinedBy(alive);
            case OCCUPIED -> alive > 0 ? this : this.in(Phase.READY);
            case WON, LOST -> this;
        };
    }

    /**
     * The round once enemies no wave sent may have joined the board between ticks. Only that: a mob
     * killed between ticks runs its on-death abilities on the next tick, so a clear waits for it.
     */
    RoundState joinedBy(int alive) {
        return this.phase == Phase.READY && alive > 0 ? this.in(Phase.OCCUPIED) : this;
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
        Map<Integer, Integer> paid = new HashMap<>(this.bonusesPaid);
        paid.keySet().removeIf(round -> round >= next - 1);
        return new RoundState(next - 1, Phase.READY, earlier, this.leaksAtStart, paid);
    }

    /** Round {@code round} (0-based) paid {@code bonus} for clearing with no leak. */
    RoundState paidBonus(int round, int bonus) {
        Map<Integer, Integer> paid = new HashMap<>(this.bonusesPaid);
        paid.put(round, bonus);
        return new RoundState(this.wave, this.phase, this.leaked, this.leaksAtStart, paid);
    }

    /** Marks the running round as one an enemy escaped, if one has since it started. */
    private RoundState noted(long leaks) {
        if (this.phase != Phase.RUNNING || leaks <= this.leaksAtStart) {
            return this;
        }
        Set<Integer> more = new HashSet<>(this.leaked);
        more.add(this.wave - 1);
        return new RoundState(this.wave, this.phase, more, this.leaksAtStart, this.bonusesPaid);
    }

    private RoundState in(Phase next) {
        return new RoundState(this.wave, next, this.leaked, this.leaksAtStart, this.bonusesPaid);
    }
}
