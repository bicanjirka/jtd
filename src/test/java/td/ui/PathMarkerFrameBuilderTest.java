package td.ui;

import org.junit.jupiter.api.Test;
import td.ui.render.PathMarkerDraw;
import td.ui.render.PathMarkerShape;
import td.wave.PathColor;
import td.wave.PathNormal;
import td.wave.Vec2;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
 * Headless and clock-free: animationSeconds is passed in explicitly, the same way other
 * tests pass explicit tick numbers rather than relying on real timing (see CLAUDE.md's
 * "Tests are headless and clock-free").
 */
class PathMarkerFrameBuilderTest {

    private static final int SCALE = 32;

    // Cell coordinates in, converted to pixel-space cell centers the same way production
    // code (PathBuilder) does, since PathNormal stores pixel points directly.
    private static PathNormal pathOf(int... xyPairs) {
        List<Vec2> points = new ArrayList<>();
        for (int i = 0; i < xyPairs.length; i += 2) {
            int x = xyPairs[i];
            int y = xyPairs[i + 1];
            points.add(new Vec2(x * SCALE + (SCALE / 2), y * SCALE + (SCALE / 2)));
        }
        return new PathNormal(points);
    }

    private static PathNormal straightPath(int fromX, int toX) {
        return pathOf(fromX, 0, toX, 0);
    }

    private static List<PathMarkerDraw> moving(List<PathMarkerDraw> markers) {
        return markers.stream().filter(m -> m.shape() == PathMarkerShape.CHEVRON).toList();
    }

    private static List<PathMarkerDraw> stationary(List<PathMarkerDraw> markers) {
        return markers.stream().filter(m -> m.shape() == PathMarkerShape.DOT).toList();
    }

    @Test
    void aPathWithFewerThanTwoStepsYieldsNoMarkers() {
        PathNormal singleStep = pathOf(0, 0);

        assertThat(PathMarkerFrameBuilder.build(singleStep, PathColor.DEFAULT, SCALE, 0.0)).isEmpty();
    }

    @Test
    void bothLayersAppearOnAStraightPath() {
        List<PathMarkerDraw> markers = PathMarkerFrameBuilder.build(straightPath(0, 10), PathColor.DEFAULT, SCALE, 0.0);

        assertThat(stationary(markers)).isNotEmpty();
        assertThat(moving(markers)).isNotEmpty();
    }

    @Test
    void staticMarkersDoNotMoveOverTime() {
        List<Float> xsAtZero = stationary(PathMarkerFrameBuilder.build(straightPath(0, 10), PathColor.DEFAULT, SCALE, 0.0))
                .stream().map(PathMarkerDraw::x).sorted().toList();
        List<Float> xsAtFive = stationary(PathMarkerFrameBuilder.build(straightPath(0, 10), PathColor.DEFAULT, SCALE, 5.0))
                .stream().map(PathMarkerDraw::x).sorted().toList();

        assertThat(xsAtFive).isEqualTo(xsAtZero);
    }

    @Test
    void movingMarkersAdvanceByPaceTimesElapsedSecondsAndWrap() {
        // a path just short of two marker-spacings long places exactly one moving marker,
        // which starts at distance 0 (see PathMarkerFrameBuilder.addLayer)
        PathNormal path = straightPath(0, 2);
        List<PathMarkerDraw> atZero = moving(PathMarkerFrameBuilder.build(path, PathColor.DEFAULT, SCALE, 0.0));
        assertThat(atZero).hasSize(1);
        float startX = atZero.getFirst().x();

        float paceCellsPerSecond = 0.4f; // MOVING_CELLS_PER_SECOND in PathMarkerFrameBuilder
        float pacePxPerSecond = paceCellsPerSecond * SCALE;
        double smallElapsed = 1.0; // small enough not to have wrapped yet
        List<PathMarkerDraw> shortlyAfter = moving(PathMarkerFrameBuilder.build(path, PathColor.DEFAULT, SCALE, smallElapsed));
        assertThat(shortlyAfter.getFirst().x())
                .isCloseTo(startX + pacePxPerSecond * (float) smallElapsed, offset(0.01f));

        double totalLength = 2 * SCALE;
        double wrappingElapsed = (totalLength / pacePxPerSecond) + 0.5;
        List<PathMarkerDraw> afterWrap = moving(PathMarkerFrameBuilder.build(path, PathColor.DEFAULT, SCALE, wrappingElapsed));
        // cell-centered pixel bounds of a 0..2 straight path at this scale: half-cell offset
        // to the last cell's center
        float pathStartPixelX = 0.5f * SCALE;
        float pathEndPixelX = 2 * SCALE + 0.5f * SCALE;
        assertThat(afterWrap.getFirst().x()).isBetween(pathStartPixelX, pathEndPixelX);
    }

    @Test
    void facingMatchesTheSegmentDirectionOnBothSidesOfACorner() {
        // first segment points +x (facing 0 rad); second points +y (facing +90 deg)
        PathNormal corner = pathOf(0, 0, 5, 0, 5, 5);

        List<Double> distinctFacings = PathMarkerFrameBuilder.build(corner, PathColor.DEFAULT, SCALE, 0.0).stream()
                .map(PathMarkerDraw::facingRadians)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();

        assertThat(distinctFacings).anySatisfy(f -> assertThat(f).isCloseTo(0.0, offset(0.001)));
        assertThat(distinctFacings).anySatisfy(f -> assertThat(f).isCloseTo(Math.PI / 2, offset(0.001)));
    }

    @Test
    void supportsANonOrthogonalDiagonalPath() {
        PathNormal diagonal = pathOf(0, 0, 3, 3);

        List<PathMarkerDraw> markers = PathMarkerFrameBuilder.build(diagonal, PathColor.DEFAULT, SCALE, 0.0);

        assertThat(markers).isNotEmpty();
        for (PathMarkerDraw marker : markers) {
            assertThat(marker.facingRadians()).isCloseTo(Math.PI / 4, offset(0.001));
            // every marker sits on the diagonal line y == x, since both endpoints do too
            assertThat((double) marker.y()).isCloseTo(marker.x(), offset(0.5));
        }
    }
}
