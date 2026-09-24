package td.wave;

import org.junit.jupiter.api.Test;
import td.level.BuiltInLevelCatalog;
import td.level.LevelDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PathCoverageTest {

    private static final int SCALE = 32;

    // comparison baseline: the old corner-expansion algorithm
    private static List<Point> expandThroughCornersLikeTheOldLevelPathDid(List<Point> corners) {
        List<Point> steps = new ArrayList<>();
        steps.add(corners.getFirst());
        for (int i = 1; i < corners.size(); i++) {
            Point from = corners.get(i - 1);
            Point to = corners.get(i);
            int stepX = Integer.signum(to.x() - from.x());
            int stepY = Integer.signum(to.y() - from.y());
            int x = from.x();
            int y = from.y();
            while (x != to.x() || y != to.y()) {
                x += stepX;
                y += stepY;
                steps.add(new Point(x, y));
            }
        }
        return steps;
    }

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
    void aDiagonalSegmentCoversTheCellsAlongItsActualLineNotJustAxisAlignedNeighbors() {
        // a 45-degree diagonal: buildability follows the real geometry, not just axis-aligned legs
        List<Vec2> polyline = List.of(new Vec2(16, 16), new Vec2(144, 144));

        Set<Point> covered = PathCoverage.unbuildableCells(polyline, SCALE, 4, 4);

        assertThat(covered).contains(new Point(0, 0), new Point(1, 1), new Point(2, 2), new Point(3, 3));
        assertThat(covered).doesNotContain(new Point(0, 3), new Point(3, 0));
    }

    @Test
    void nothingIsCoveredWhenThePathsBoundingBoxDoesNotReachTheGrid() {
        List<Vec2> polyline = List.of(new Vec2(5000, 5000), new Vec2(6000, 6000));

        Set<Point> covered = PathCoverage.unbuildableCells(polyline, SCALE, 5, 5);

        assertThat(covered).isEmpty();
    }

    /**
     * The load-bearing regression test for this whole rewrite: proves that for a real,
     * shipped level (Curly Path), the sparse corner-only path this change introduces covers
     * *exactly* the same cells as the old dense, one-cell-per-step path did. {@code
     * expandThroughCornersLikeTheOldLevelPathDid} is a frozen copy of the axis-aligned
     * expansion {@code LevelPath.throughCorners} used to perform before it was deleted - it
     * exists only as a comparison baseline in this test, not as production code.
     */
    @Test
    void curlyPathsSparseCornersCoverTheSameCellsAsTheOldDenseExpansionDid() {
        LevelDefinition curlyPath = new BuiltInLevelCatalog().levels().getFirst();
        List<Point> corners = curlyPath.paths().getFirst().corners();

        List<Vec2> oldDensePolyline = expandThroughCornersLikeTheOldLevelPathDid(corners).stream()
                .map(cell -> new Vec2(cell.x() * SCALE + (SCALE / 2), cell.y() * SCALE + (SCALE / 2)))
                .toList();
        Set<Point> coveredByOldDensePath = PathCoverage.unbuildableCells(
                oldDensePolyline, SCALE, curlyPath.width(), curlyPath.height());

        List<Vec2> sparsePolyline = corners.stream()
                .map(cell -> new Vec2(cell.x() * SCALE + (SCALE / 2), cell.y() * SCALE + (SCALE / 2)))
                .toList();
        Set<Point> coveredBySparsePath = PathCoverage.unbuildableCells(
                sparsePolyline, SCALE, curlyPath.width(), curlyPath.height());

        assertThat(coveredBySparsePath).containsExactlyInAnyOrderElementsOf(coveredByOldDensePath);
    }
}
