package td;

import org.junit.jupiter.api.Test;
import td.economy.EconomyState;
import td.fixtures.BoardFixtures;
import td.level.BuiltInLevelCatalog;
import td.level.LevelDefinition;
import td.level.LevelOutcome;
import td.tower.SniperTower;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.util.RandomSource;
import td.wave.Path;
import td.wave.PathBuilder;
import td.wave.PathCoverage;
import td.wave.PathDefinition;
import td.wave.Point;
import td.wave.Vec2;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loads and plays every built-in level through the engine, proving curved paths work in play, not
 * just in geometry.
 */
class BuiltInLevelCatalogEngineTest {

    private static final int UNLOSABLE_LIVES = 1_000_000;
    private static final int TICK_BUDGET = 500_000;
    private static final long RANDOM_SEED = 20260924L;

    /** A cell only the smoothed curve covers, not the raw corners. */
    private static Point aCellOnlyTheSmoothedCurveCovers(LevelDefinition level, PathDefinition path) {
        List<Vec2> rawPolyline = path.corners().stream()
                .map(cell -> new Vec2(cell.x() * BoardFixtures.SCALE + (BoardFixtures.SCALE / 2.0),
                        cell.y() * BoardFixtures.SCALE + (BoardFixtures.SCALE / 2.0)))
                .toList();
        Set<Point> straightCornerCoverage = PathCoverage.unbuildableCells(
                rawPolyline, BoardFixtures.SCALE, level.width(), level.height());

        Path smoothedPath = PathBuilder.build(path.corners(), path.smoothing(), BoardFixtures.SCALE);
        Set<Point> smoothedCoverage = PathCoverage.unbuildableCells(
                smoothedPath.points(), BoardFixtures.SCALE, level.width(), level.height());

        return smoothedCoverage.stream()
                .filter(cell -> !straightCornerCoverage.contains(cell))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected the smoothed curve to cover at least one cell the raw corners don't"));
    }

    /**
     * Plays every wave of every built-in level with no towers and lives that cannot run out, so
     * each wave script spawns and walks its whole path whatever the balance numbers are.
     */
    @Test
    void everyBuiltInLevelPlaysThroughEveryWaveToAWinWhenLivesCannotRunOut() {
        for (LevelDefinition builtIn : new BuiltInLevelCatalog().levels()) {
            LevelDefinition level = builtIn.withStartingLives(UNLOSABLE_LIVES);
            GameEngine engine = FakeGameHost.newBoundEngine(RandomSource.seeded(RANDOM_SEED));
            engine.loadLevel(level);
            boolean livesEverRose = false;
            int lowestCredits = level.startingCredits();

            int t = 1;
            for (int previousLives = UNLOSABLE_LIVES; t <= TICK_BUDGET && !engine.outcome().isOver(); t++) {
                engine.nextWave();
                engine.doTick(t);
                EconomyState state = engine.getGameWorld().economy().state();
                livesEverRose |= state.lives() > previousLives;
                lowestCredits = Math.min(lowestCredits, state.credits());
                previousLives = state.lives();
            }

            assertThat(engine.outcome()).describedAs("level '%s' after %d ticks", level.name(), t)
                    .isEqualTo(LevelOutcome.WON);
            assertThat(engine.getCurrentWaveIndex()).isEqualTo(engine.getWaveCount());
            assertThat(livesEverRose).describedAs("level '%s' lives rose", level.name()).isFalse();
            assertThat(lowestCredits).describedAs("level '%s' lowest credits", level.name()).isNotNegative();
        }
    }

    @Test
    void twistedHourglassesCurvedCorridorBlocksBuildingOnACellTheRawStraightCornersNeverCovered() {
        LevelDefinition level = new BuiltInLevelCatalog().levels().get(2);
        PathDefinition amberPath = level.paths().get(2);
        Point curveOnlyCell = aCellOnlyTheSmoothedCurveCovers(level, amberPath);

        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(level);
        int creditsBefore = engine.getGameWorld().economy().getCredits();

        engine.startPlacing(TowerFactory.Type.SNIPER, SniperTower.RANGE);
        Optional<Tower> selected = engine.mouseClicked(BoardFixtures.cellCenter(curveOnlyCell.x()), BoardFixtures.cellCenter(curveOnlyCell.y()));

        assertThat(selected).isEmpty();
        assertThat(engine.cells().at(curveOnlyCell.x(), curveOnlyCell.y()).hasTower()).isFalse();
        assertThat(engine.getGameWorld().economy().getCredits()).isEqualTo(creditsBefore);
    }

    @Test
    void twistedHourglassesFarCornerAwayFromTheCurveIsStillBuildable() {
        LevelDefinition level = new BuiltInLevelCatalog().levels().get(2);
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(level);

        engine.startPlacing(TowerFactory.Type.SNIPER, SniperTower.RANGE);
        engine.mouseClicked(BoardFixtures.cellCenter(level.width() - 1), BoardFixtures.cellCenter(level.height() - 1));

        assertThat(engine.cells().at(level.width() - 1, level.height() - 1).hasTower()).isTrue();
    }
}
