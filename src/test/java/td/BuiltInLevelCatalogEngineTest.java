package td;

import org.junit.jupiter.api.Test;
import td.level.BuiltInLevelCatalog;
import td.level.LevelDefinition;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.tower.TowerOne;
import td.wave.Path;
import td.wave.PathBuilder;
import td.wave.PathCoverage;
import td.wave.Point;
import td.wave.Vec2;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Drives every catalog level (Classic Loop, Zigzag Gauntlet, Wild Bezier Sweep) through the same
 * GameEngine entry points TowerDefence's real listeners call, proving a new level with an
 * aggressively curved path (see Wild Bezier Sweep's cornerPull=0.5) is safe to load and play, not
 * just a geometrically valid LevelDefinition.
 */
class BuiltInLevelCatalogEngineTest {

    private static final int SCALE = 32;

    @Test
    void everyBuiltInLevelLoadsAndRunsThroughItsFirstWaveWithoutError() {
        for (LevelDefinition level : new BuiltInLevelCatalog().levels()) {
            GameEngine engine = FakeGameHost.newBoundEngine();

            assertThatCode(() -> engine.loadLevel(level)).doesNotThrowAnyException();

            engine.requestNextWave();
            assertThatCode(() -> {
                for (int t = 1; t <= 90; t++) {
                    if (engine.isWaveReady()) {
                        engine.nextWave();
                    }
                    engine.doTick(t);
                }
            }).describedAs("level '%s'", level.name()).doesNotThrowAnyException();
        }
    }

    @Test
    void wildBezierSweepsCurvedCorridorBlocksBuildingOnACellTheRawStraightCornersNeverCovered() {
        LevelDefinition level = new BuiltInLevelCatalog().levels().get(2);
        Point curveOnlyCell = aCellOnlyTheSmoothedCurveCovers(level);

        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(level);
        int creditsBefore = engine.getGameWorld().getCredits();

        engine.startPlacing(TowerFactory.type.first, TowerOne.range);
        Tower selected = engine.mouseClicked(cellCenter(curveOnlyCell.x()), cellCenter(curveOnlyCell.y()));

        assertThat(selected).isNull();
        assertThat(engine.getCellGrid()[curveOnlyCell.x()][curveOnlyCell.y()].hasTower()).isFalse();
        assertThat(engine.getGameWorld().getCredits()).isEqualTo(creditsBefore);
    }

    @Test
    void wildBezierSweepsFarCornerAwayFromTheCurveIsStillBuildable() {
        LevelDefinition level = new BuiltInLevelCatalog().levels().get(2);
        GameEngine engine = FakeGameHost.newBoundEngine();
        engine.loadLevel(level);

        engine.startPlacing(TowerFactory.type.first, TowerOne.range);
        engine.mouseClicked(cellCenter(0), cellCenter(level.height() - 1));

        assertThat(engine.getCellGrid()[0][level.height() - 1].hasTower()).isTrue();
    }

    /**
     * Finds a cell the smoothed (curved) path marks unbuildable that the same level's raw,
     * unsmoothed corners would not - i.e. a cell only the wild Bezier sweep itself reaches.
     */
    private static Point aCellOnlyTheSmoothedCurveCovers(LevelDefinition level) {
        List<Vec2> rawPolyline = level.path().stream()
                .map(cell -> new Vec2(cell.x() * SCALE + (SCALE / 2.0), cell.y() * SCALE + (SCALE / 2.0)))
                .toList();
        Set<Point> straightCornerCoverage = PathCoverage.unbuildableCells(rawPolyline, SCALE, level.width(), level.height());

        Path smoothedPath = PathBuilder.build(level.path(), level.smoothing(), SCALE);
        Set<Point> smoothedCoverage = PathCoverage.unbuildableCells(smoothedPath.points(), SCALE, level.width(), level.height());

        return smoothedCoverage.stream()
                .filter(cell -> !straightCornerCoverage.contains(cell))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected the smoothed curve to cover at least one cell the raw corners don't"));
    }

    private static int cellCenter(int cellIndex) {
        return cellIndex * SCALE + SCALE / 2;
    }
}
