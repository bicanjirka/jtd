package td.wave;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PathNormalTest {

    @Test
    void beforeFinaliseLengthIsZero() {
        PathNormal path = new PathNormal(10);

        assertThat(path.length()).isZero();
    }

    @Test
    void beforeFinaliseGetStepReturnsOrigin() {
        PathNormal path = new PathNormal(10);

        assertThat(path.getStep(0)).isEqualTo(new Point(0, 0));
        assertThat(path.getStep(-5)).isEqualTo(new Point(0, 0));
        assertThat(path.getStep(99)).isEqualTo(new Point(0, 0));
    }

    @Test
    void getStepReturnsPixelCenterOfCell() {
        PathNormal path = new PathNormal(10);
        path.addStep(2, 3);
        path.finalise(new RecordingCell[1][1]);

        // pixel center = coordinate * scale + scale/2
        assertThat(path.getStep(0)).isEqualTo(new Point(25, 35));
    }

    @Test
    void negativeStepClampsToFirstStep() {
        PathNormal path = new PathNormal(10);
        path.addStep(2, 3);
        path.addStep(4, 5);
        path.finalise(new RecordingCell[1][1]);

        assertThat(path.getStep(-1)).isEqualTo(path.getStep(0));
    }

    @Test
    void stepPastEndClampsToLastStep() {
        PathNormal path = new PathNormal(10);
        path.addStep(2, 3);
        path.addStep(4, 5);
        path.finalise(new RecordingCell[1][1]);

        assertThat(path.getStep(99)).isEqualTo(path.getStep(1));
    }

    @Test
    void lengthReflectsAddedStepsAfterFinalise() {
        PathNormal path = new PathNormal(10);
        // coordinates chosen outside the 1x1 grid below, so finalise()
        // never dereferences a (deliberately unpopulated) grid cell
        path.addStep(5, 5);
        path.addStep(6, 6);
        path.addStep(7, 7);
        path.finalise(new RecordingCell[1][1]);

        assertThat(path.length()).isEqualTo(3);
    }

    @Test
    void finaliseDisablesInBoundsPathCellsAndLeavesOthersUntouched() {
        PathNormal path = new PathNormal(10);
        path.addStep(0, 0);
        path.addStep(1, 1);

        RecordingCell[][] grid = new RecordingCell[3][3];
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                grid[x][y] = new RecordingCell();
            }
        }
        path.finalise(grid);

        assertThat(grid[0][0].wasDisabled()).isTrue();
        assertThat(grid[1][1].wasDisabled()).isTrue();
        assertThat(grid[2][2].enableWasCalled()).isFalse();
        assertThat(grid[0][1].enableWasCalled()).isFalse();
    }

    @Test
    void finaliseIgnoresStepsOutsideGridBounds() {
        PathNormal path = new PathNormal(10);
        path.addStep(-1, -1);
        path.addStep(50, 50);

        // no exception even though both steps are out of a 1x1 grid's bounds
        path.finalise(new RecordingCell[1][1]);

        assertThat(path.length()).isEqualTo(2);
    }
}
