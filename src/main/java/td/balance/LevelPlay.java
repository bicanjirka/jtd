package td.balance;

import td.GameEngine;
import td.economy.EconomyState;
import td.level.LevelDefinition;
import td.save.GameSave;
import td.util.GameHost;
import td.util.RandomSource;
import td.util.ThreadConfined;

import java.util.List;
import java.util.Optional;

/**
 * Plays one level headlessly the way a person does: the player acts before each round and every second during
 * one, calls the next round after a short pause, and the level ends when every round is cleared or time runs
 * out. Lives never run out: the play is identical to the real one up to the leak that would have lost it, so one
 * play gives both whether the level was held and by how much it was not. The engine and the player each get
 * their own randomness from the seed, so a play repeats exactly.
 */
@ThreadConfined(value = ThreadConfined.Owner.ENCLOSING)
public final class LevelPlay {

    /**
     * How a play is paced, and how long it is watched.
     *
     * @param pauseTicks    between a round clearing and the next being called: what a person spends reading the
     *                      preview, and what a tower that charges between rounds gets
     * @param decisionTicks how often the player may act during a round
     * @param tickBudget    a play still running after this many ticks ends as it stands
     * @param stopAtLoss    end the play at the leak that loses the real level, when only the verdict is wanted
     */
    public record Pacing(int pauseTicks, int decisionTicks, int tickBudget, boolean stopAtLoss) {

        public static Pacing standard() {
            return new Pacing(60, 20, 200_000, false);
        }

        public Pacing stoppingAtLoss() {
            return new Pacing(this.pauseTicks, this.decisionTicks, this.tickBudget, true);
        }

        public Pacing withPauseTicks(int ticks) {
            return new Pacing(ticks, this.decisionTicks, this.tickBudget, this.stopAtLoss);
        }

        Pacing playingOut() {
            return new Pacing(this.pauseTicks, this.decisionTicks, this.tickBudget, false);
        }
    }

    private static final long PLAYER_SEED_SALT = 0x5DEECE66DL;
    private static final int UNLIMITED_LIVES = 1_000_000;
    /** Spreads the seeds of plays loaded from a save apart from those of the same seed's whole plays. */
    private static final long SAVE_SEED_STEP = 31L;

    private LevelPlay() {
    }

    /** Plays {@code level} once with {@code spec}'s player and returns everything the play left behind. */
    public static PlayRecord play(BoardSurvey survey, PlayerSpec spec, long seed, Pacing pacing) {
        return play(survey, spec, seed, pacing, Rules.standard());
    }

    /** {@link #play(BoardSurvey, PlayerSpec, long, Pacing)} under {@code rules}. */
    public static PlayRecord play(BoardSurvey survey, PlayerSpec spec, long seed, Pacing pacing, Rules rules) {
        return play(survey, spec.createFor(survey, seed), spec.name(), seed, pacing, rules,
                new Span(Optional.empty(), 0)).record();
    }

    /** {@link #play(BoardSurvey, PlayerSpec, long, Pacing, Rules)} from {@code from}, if any, to the end. */
    static PlayRecord play(BoardSurvey survey, PlayerSpec spec, long seed, Pacing pacing, Rules rules,
                           Optional<GameSave> from) {
        return play(survey, spec.createFor(survey, seed), spec.name(), seed, pacing, rules, new Span(from, 0))
                .record();
    }

    /** {@link #play(BoardSurvey, PlayerSpec, long, Pacing)}, stopping once round {@code lastRound} is over. */
    static PlayRecord playThrough(BoardSurvey survey, PlayerSpec spec, long seed, Pacing pacing, int lastRound) {
        return play(survey, spec.createFor(survey, seed), spec.name(), seed, pacing, Rules.standard(),
                new Span(Optional.empty(), lastRound)).record();
    }

    /** The game {@code actions} leave just before round {@code wave}, its own purchases before it not made. */
    static GameSave saveBefore(BoardSurvey survey, List<PlayRecord.TimedAction> actions, long seed, int wave) {
        Player player = new ScriptedPlayer(actions.stream().filter(action -> action.round() < wave).toList());
        Played played = play(survey, player, "save", seed, Pacing.standard(), Rules.standard(),
                new Span(Optional.empty(), wave - 1));
        return realLives(played.engine().save().orElseThrow(), survey.level().startingLives());
    }

    /** {@code save} with the economy's lives set to {@code lives}. */
    private static GameSave withLives(GameSave save, int lives) {
        return new GameSave(save.level(), save.nextWave(),
                new EconomyState(save.economy().credits(), save.economy().score(), lives), save.towers());
    }

    /** A save of a play whose lives never run out, with the lives the real play would have left of {@code lives}. */
    private static GameSave realLives(GameSave save, int lives) {
        return withLives(save, Math.max(0, lives - (UNLIMITED_LIVES - save.economy().lives())));
    }

    /**
     * Where a play starts and ends.
     *
     * @param from    the save it starts from, or none for the first round
     * @param through the last round it plays, 0 for the level out
     */
    private record Span(Optional<GameSave> from, int through) {
    }

    private record Played(PlayRecord record, GameEngine engine) {
    }

    /**
     * Plays with {@code player} through {@code span}, from its save or from the first tick. A player that rehearses
     * replays the level through this from where this play started, with the same seed, pacing and rules.
     */
    private static Played play(BoardSurvey survey, Player player, String name, long seed, Pacing pacing,
                               Rules rules, Span span) {
        LevelDefinition level = rules.applyTo(survey.level());
        int lives = span.from().map(save -> save.economy().lives()).orElse(level.startingLives());
        GameEngine[] holder = new GameEngine[1];
        GameHost host = (x, y) -> holder[0].clearCell(x, y);
        GameEngine engine = new GameEngine(host, RandomSource.seeded(span.from()
                .map(save -> seed * SAVE_SEED_STEP + save.nextWave()).orElse(seed)));
        holder[0] = engine;
        // Played on past the last life: up to the leak that would have lost, the play is the same, and the leaks
        // after it measure by how much.
        engine.loadLevel(level.withStartingLives(UNLIMITED_LIVES));
        rules.applyTo(engine);
        // A save that loads only in part would play the waves with a weaker army and blame the waves.
        span.from().flatMap(save -> engine.restore(withLives(save, UNLIMITED_LIVES))).ifPresent(problem -> {
            throw new IllegalStateException("The save does not fit the game any more: " + problem);
        });
        RunRecorder recorder = new RunRecorder(engine, lives);
        Turn.Rehearsal rehearsal = (alternative, through) -> play(survey, alternative, name, seed,
                pacing.playingOut(), rules, new Span(span.from(), through)).record();
        Turn turn = new Turn(engine, recorder, survey, RandomSource.seeded(seed ^ PLAYER_SEED_SALT), rehearsal);

        boolean paused = false;
        int pauseStart = 0;
        int roundStart = 0;
        for (int t = 1; t <= pacing.tickBudget() && !engine.outcome().isOver()
                && !(pacing.stopAtLoss() && recorder.leaks() >= lives)
                && !(span.through() > 0 && recorder.roundsOver() >= span.through()); t++) {
            if (engine.isWaveReady()) {
                if (!paused) {
                    paused = true;
                    pauseStart = t;
                    turn.at(engine.getCurrentWaveIndex() + 1, PlayRecord.TimedAction.BEFORE_ROUND);
                    player.act(turn);
                }
                if (t - pauseStart >= pacing.pauseTicks()) {
                    engine.requestNextWave();
                }
            } else if ((t - roundStart) % pacing.decisionTicks() == 0) {
                turn.at(engine.getCurrentWaveIndex(), t - roundStart);
                player.act(turn);
            }
            if (engine.doTick(t)) {
                paused = false;
                roundStart = t;
                recorder.roundStarted(t);
            }
            recorder.afterTick(t);
        }
        return new Played(recorder.finish(new PlayRecord.Play(level.name(), seed, name)), engine);
    }
}
