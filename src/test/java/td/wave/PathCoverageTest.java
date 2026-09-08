package td.wave;

import org.junit.jupiter.api.Test;
import td.level.BuiltInLevelCatalog;
import td.level.LevelDefinition;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class PathCoverageTest {

    private static final int SCALE = 32;

    @Test
    void aPathWithFewerThanTwoPointsCoversNoCells() {
        Set<Point> covered = PathCoverage.unbuildableCells(List.of(new Vec2(16, 16)), SCALE, 5, 5);

        assertThat(covered).isEmpty();
    }

    @Test
    void aStraightHorizontalSegmentCoversExactlyTheCellsItPassesThrough() {
        // cell-center to cell-center, row 0, cells 0 through 2
        List<Vec2> polyline = List.of(new Vec2(16, 16), new Vec2(80, 16));

        Set<Point> covered = PathCoverage.unbuildableCells(polyline, SCALE, 5, 5);

        assertThat(covered).containsExactlyInAnyOrder(new Point(0, 0), new Point(1, 0), new Point(2, 0));
    }

    @Test
    void aRightAngleCornerCoversTheCornerCellToo() {
        // (cell 0,0) -> (cell 2,0) -> (cell 2,2): the corner cell (2,0) is where both legs meet
        List<Vec2> polyline = List.of(new Vec2(16, 16), new Vec2(80, 16), new Vec2(80, 80));

        Set<Point> covered = PathCoverage.unbuildableCells(polyline, SCALE, 5, 5);

        assertThat(covered).contains(new Point(0, 0), new Point(1, 0), new Point(2, 0), new Point(2, 1), new Point(2, 2));
    }

    @Test
    void cellsWellOffThePathAreNotCovered() {
        List<Vec2> polyline = List.of(new Vec2(16, 16), new Vec2(80, 16));

        Set<Point> covered = PathCoverage.unbuildableCells(polyline, SCALE, 5, 5);

        assertThat(covered).doesNotContain(new Point(0, 2), new Point(2, 4), new Point(4, 4));
    }

    @Test
    void nothingIsCoveredWhenThePathsBoundingBoxDoesNotReachTheGrid() {
        List<Vec2> polyline = List.of(new Vec2(5000, 5000), new Vec2(6000, 6000));

        Set<Point> covered = PathCoverage.unbuildableCells(polyline, SCALE, 5, 5);

        assertThat(covered).isEmpty();
    }

    /**
     * The load-bearing regression test for this whole rewrite: proves that for a real,
     * unsmoothed level's path - one grid cell per listed step, exactly like
     * LevelPath.throughCorners has always produced - the new coverage-based algorithm marks
     * *exactly* the same cells unbuildable as the old "mark the listed cells" one did, no more
     * and no fewer, for both built-in levels. The old algorithm also bounds-checked each step
     * before marking it (a level's path deliberately starts/ends off-board), so "its own
     * cells" here means the ones actually within the grid, same as before.
     */
    @Test
    void classicLoopsUnsmoothedPathCoversExactlyTheCellsItWasAuthoredThrough() {
        assertCoversExactlyItsOwnCells(new BuiltInLevelCatalog().levels().get(0));
    }

    @Test
    void zigzagGauntletsUnsmoothedPathCoversExactlyTheCellsItWasAuthoredThrough() {
        assertCoversExactlyItsOwnCells(new BuiltInLevelCatalog().levels().get(1));
    }

    private static void assertCoversExactlyItsOwnCells(LevelDefinition level) {
        List<Vec2> pixelPolyline = level.path().stream()
                .map(cell -> new Vec2(cell.x() * SCALE + (SCALE / 2), cell.y() * SCALE + (SCALE / 2)))
                .toList();

        Set<Point> covered = PathCoverage.unbuildableCells(pixelPolyline, SCALE, level.width(), level.height());

        Set<Point> expectedInBounds = level.path().stream()
                .filter(p -> p.x() >= 0 && p.x() < level.width() && p.y() >= 0 && p.y() < level.height())
                .collect(Collectors.toSet());
        assertThat(covered).containsExactlyInAnyOrderElementsOf(expectedInBounds);
    }
}
