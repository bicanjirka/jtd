package td.balance;

import td.GameEngine;
import td.cell.CellGrid;
import td.damage.DamageMix;
import td.level.LevelDefinition;
import td.level.LevelOutcome;
import td.util.GameHost;
import td.util.RandomSource;
import td.wave.ArcLengthPath;
import td.wave.PathDefinition;
import td.wave.Point;
import td.wave.Vec2;
import td.wave.WaveDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleFunction;

/**
 * Measures enemies against a {@link ProbeRig} instead of towers: a round walks a board with no tower on it while the
 * rig lands a loadout, and the measure is the weakest loadout that lets nothing through. It runs the real engine, so
 * heals, shields, spawns, hiding and dodging count for what they do, and how fast an enemy walks counts as the time
 * the rig has.
 */
final class PowerBench {

    /** How long the straight lane of a {@link #lane} is. */
    static final int LANE_CELLS = 14;
    private static final int LANE_HEIGHT = 3;
    private static final int UNLIMITED_LIVES = 1_000_000;
    private static final int TICK_BUDGET = 40_000;
    private static final int SEEDS = 3;
    private static final long FIRST_SEED = 1L;
    /** Above this a loadout is taken to never get through: the enemy is immune to it. */
    private static final double MOST = 500_000;
    private static final double TOLERANCE = 0.03;
    /** A cell this near a lane is one a tower would be built on. */
    private static final double BESIDE_CELLS = 1.6;

    private PowerBench() {
    }

    /**
     * A board that is one straight lane with one round, {@code script}, of {@code content}'s enemies: its own and
     * the built-in ones.
     */
    static LevelDefinition lane(LevelDefinition content, String script) {
        PathDefinition path = PathDefinition.of(List.of(new Point(-1, 1), new Point(LANE_CELLS, 1)),
                List.of(new WaveDefinition(script)));
        return LevelDefinition.of("Power lane", LANE_CELLS, LANE_HEIGHT, List.of(path))
                .withCustomEnemies(content.customEnemies())
                .withCustomRankedEnemies(content.customRankedEnemies())
                .withStartingCredits(0)
                .withStartingLives(UNLIMITED_LIVES);
    }

    /** {@code level} with only its round {@code round}, on every lane it has. */
    static LevelDefinition oneRound(LevelDefinition level, int round) {
        List<PathDefinition> paths = level.paths().stream()
                .map(path -> path.withWaves(List.of(new WaveDefinition(path.waves().get(round).enemies()))))
                .toList();
        return LevelDefinition.of(level.name(), level.width(), level.height(), paths)
                .withCustomEnemies(level.customEnemies())
                .withCustomRankedEnemies(level.customRankedEnemies())
                .withStartingCredits(0)
                .withStartingLives(UNLIMITED_LIVES);
    }

    /**
     * The smallest {@code scale} at which {@code loadoutAt} lets nothing of {@code level}'s first round through,
     * averaged over the seeds; infinite when no loadout within reason does.
     *
     * @param start  a scale to try first
     * @param before the damage the level has seen before the round, which an enemy that adapts reads
     */
    static double threshold(LevelDefinition level, DoubleFunction<ProbeRig.Loadout> loadoutAt, double start,
                            DamageMix before) {
        double sum = 0;
        for (long seed = FIRST_SEED; seed < FIRST_SEED + SEEDS; seed++) {
            double low = 0;
            double high = start;
            while (run(level, loadoutAt.apply(high), before, seed).leaked()) {
                low = high;
                high *= 2;
                if (high > MOST * start) {
                    return Double.POSITIVE_INFINITY;
                }
            }
            while (high - low > TOLERANCE * high) {
                double middle = (low + high) / 2;
                if (run(level, loadoutAt.apply(middle), before, seed).leaked()) {
                    low = middle;
                } else {
                    high = middle;
                }
            }
            sum += high;
        }
        return sum / SEEDS;
    }

    /** Plays {@code level}'s first round under {@code loadout} until something leaks or nothing is left. */
    static Outcome run(LevelDefinition level, ProbeRig.Loadout loadout, DamageMix before, long seed) {
        GameEngine engine = new GameEngine(GameHost.noOp(), RandomSource.seeded(seed));
        engine.loadLevel(level);
        engine.getGameWorld().damageTally().restore(before);
        engine.getGameWorld().playtestHooks().add(new ProbeRig(engine.getGameWorld(), loadout, emplacements(engine)));
        engine.requestNextWave();
        int tick = 1;
        engine.doTick(tick);
        while (tick < TICK_BUDGET && !engine.isWaveReady() && !engine.outcome().isOver()
                && engine.getGameWorld().enemies().leaks() == 0) {
            tick++;
            engine.doTick(tick);
        }
        boolean leaked = engine.getGameWorld().enemies().leaks() > 0 || engine.outcome() == LevelOutcome.LOST;
        return new Outcome(leaked, tick, engine.getGameWorld().economy().getCredits());
    }

    /** The centres of the buildable cells beside a lane: where a defence would stand. */
    private static List<Vec2> emplacements(GameEngine engine) {
        int scale = engine.getGameWorld().getBoard().scale();
        List<ArcLengthPath> lanes = engine.getGameWorld().level().paths().stream()
                .flatMap(lane -> ArcLengthPath.of(lane.path()).stream()).toList();
        List<Vec2> cells = new ArrayList<>();
        CellGrid grid = engine.cells();
        for (int x = 0; x < grid.width(); x++) {
            for (int y = 0; y < grid.height(); y++) {
                Vec2 centre = new Vec2((x + 0.5) * scale, (y + 0.5) * scale);
                if (grid.at(x, y).buildable() && lanes.stream()
                        .anyMatch(lane -> lane.nearest(centre.x(), centre.y()).distance() <= BESIDE_CELLS * scale)) {
                    cells.add(centre);
                }
            }
        }
        return cells;
    }

    /**
     * How a round under a rig ended.
     *
     * @param leaked  whether an enemy got through
     * @param ticks   when the first enemy got through, or when the last one died
     * @param credits what the kills paid
     */
    record Outcome(boolean leaked, int ticks, int credits) {
    }
}
