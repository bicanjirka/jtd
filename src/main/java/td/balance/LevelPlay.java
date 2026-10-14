package td.balance;

import td.GameEngine;
import td.level.LevelDefinition;
import td.util.GameHost;
import td.util.RandomSource;
import td.util.ThreadConfined;

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

    private LevelPlay() {
    }

    /** Plays {@code level} once with {@code spec}'s player and returns everything the play left behind. */
    public static PlayRecord play(BoardSurvey survey, PlayerSpec spec, long seed, Pacing pacing) {
        return play(survey, spec, seed, pacing, Rules.standard());
    }

    /** {@link #play(BoardSurvey, PlayerSpec, long, Pacing)} under {@code rules}. */
    public static PlayRecord play(BoardSurvey survey, PlayerSpec spec, long seed, Pacing pacing, Rules rules) {
        return play(survey, spec.createFor(survey, seed), spec.name(), seed, pacing, rules, 0);
    }

    /**
     * Plays with {@code player} from the first tick, stopping once round {@code lastRound} is over (0 plays the
     * level out). A player that rehearses replays the level through this, with the same seed, pacing and rules.
     */
    private static PlayRecord play(BoardSurvey survey, Player player, String name, long seed, Pacing pacing,
                                   Rules rules, int lastRound) {
        LevelDefinition level = rules.applyTo(survey.level());
        int lives = level.startingLives();
        GameEngine[] holder = new GameEngine[1];
        GameHost host = (x, y) -> holder[0].clearCell(x, y);
        GameEngine engine = new GameEngine(host, RandomSource.seeded(seed));
        holder[0] = engine;
        // Played on past the last life: up to the leak that would have lost, the play is the same, and the leaks
        // after it measure by how much.
        engine.loadLevel(level.withStartingLives(UNLIMITED_LIVES));
        rules.applyTo(engine);
        RunRecorder recorder = new RunRecorder(engine, lives);
        Turn.Rehearsal rehearsal = (alternative, through) -> play(survey, alternative, name, seed,
                pacing.playingOut(), rules, through);
        Turn turn = new Turn(engine, recorder, survey, RandomSource.seeded(seed ^ PLAYER_SEED_SALT), rehearsal);

        boolean paused = false;
        int pauseStart = 0;
        int roundStart = 0;
        for (int t = 1; t <= pacing.tickBudget() && !engine.outcome().isOver()
                && !(pacing.stopAtLoss() && recorder.leaks() >= lives)
                && !(lastRound > 0 && recorder.roundsOver() >= lastRound); t++) {
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
        return recorder.finish(new PlayRecord.Play(level.name(), seed, name));
    }
}
