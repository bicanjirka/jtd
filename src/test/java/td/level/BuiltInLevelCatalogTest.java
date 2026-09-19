package td.level;

import org.junit.jupiter.api.Test;
import td.wave.Path;
import td.wave.PathBuilder;
import td.wave.PathCoverage;
import td.wave.PathDefinition;
import td.wave.Point;
import td.wave.Vec2;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class BuiltInLevelCatalogTest {

    private static final int SCALE = 32;

    private static Set<Point> cellsOnlyTheSmoothedCurveCovers(LevelDefinition level) {
        Set<Point> straightCornerCoverage = PathCoverage.unbuildableCells(
                rawPixelPolyline(level), SCALE, level.width(), level.height());

        PathDefinition path = level.paths().getFirst();
        Path smoothedPath = PathBuilder.build(path.corners(), path.smoothing(), SCALE);
        Set<Point> smoothedCoverage = PathCoverage.unbuildableCells(
                smoothedPath.points(), SCALE, level.width(), level.height());

        return smoothedCoverage.stream()
                .filter(cell -> !straightCornerCoverage.contains(cell))
                .collect(Collectors.toSet());
    }

    private static LevelDefinition thirdLevel() {
        return new BuiltInLevelCatalog().levels().get(2);
    }

    private static List<Vec2> rawPixelPolyline(LevelDefinition level) {
        return level.paths().getFirst().corners().stream()
                .map(cell -> new Vec2(cell.x() * SCALE + (SCALE / 2.0), cell.y() * SCALE + (SCALE / 2.0)))
                .toList();
    }

    @Test
    void theCatalogOffersAllThreeBuiltInLevelsInOrder() {
        List<LevelDefinition> levels = new BuiltInLevelCatalog().levels();

        assertThat(levels).extracting(LevelDefinition::name)
                .containsExactly("Classic Loop", "Zigzag Gauntlet", "Wild Bezier Sweep");
    }

    /**
     * The load-bearing proof for "Wild Bezier Sweep": with cornerPull maxed at 0.5 and every leg
     * several cells long, the smoothed path's buildable corridor swings well clear of the raw,
     * unsmoothed corner-to-corner polyline - not just the handful of corner cells a gentle fillet
     * would round off. Proven relative to Zigzag Gauntlet, the one other smoothed built-in level:
     * its gentle 0.3 pull over short 2-4 cell legs never covers a single cell the raw corners
     * didn't already cover, while Wild Bezier Sweep's fillets carve out a double-digit number of
     * cells the raw corners never touch at all - exactly what "spans far beyond the original
     * path" means geometrically.
     */
    @Test
    void wildBezierSweepsSmoothedPathCoversFarMoreNewCellsThanZigzagGauntletsGentlerSmoothing() {
        int wildNewCellCount = cellsOnlyTheSmoothedCurveCovers(thirdLevel()).size();
        int zigzagNewCellCount = cellsOnlyTheSmoothedCurveCovers(new BuiltInLevelCatalog().levels().get(1)).size();

        assertThat(zigzagNewCellCount).isZero();
        assertThat(wildNewCellCount).isGreaterThanOrEqualTo(10);
    }

    @Test
    void wildBezierSweepsPathStaysEntirelyWithinItsOwnBoard() {
        LevelDefinition level = thirdLevel();
        PathDefinition path = level.paths().getFirst();

        Path smoothedPath = PathBuilder.build(path.corners(), path.smoothing(), SCALE);

        int minX = path.corners().stream().mapToInt(Point::x).min().orElseThrow();
        int maxX = path.corners().stream().mapToInt(Point::x).max().orElseThrow();
        int minY = path.corners().stream().mapToInt(Point::y).min().orElseThrow();
        int maxY = path.corners().stream().mapToInt(Point::y).max().orElseThrow();

        // The convex-hull property of a quadratic Bezier guarantees the smoothed curve never
        // leaves the bounding box of its own raw corners, however large cornerPull is.
        for (Vec2 point : smoothedPath.points()) {
            assertThat(point.x()).isBetween((double) (minX * SCALE), (double) (maxX * SCALE + SCALE));
            assertThat(point.y()).isBetween((double) (minY * SCALE), (double) (maxY * SCALE + SCALE));
        }
    }
}
