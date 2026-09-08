package td.wave;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PathNormalTest {

    private static final int SCALE = 10;

    @Test
    void beforeFinaliseLengthIsZero() {
        PathNormal path = new PathNormal(SCALE);

        assertThat(path.length()).isZero();
    }

    @Test
    void beforeFinaliseGetStepReturnsOrigin() {
        PathNormal path = new PathNormal(SCALE);

        assertThat(path.getStep(0)).isEqualTo(new Vec2(0, 0));
        assertThat(path.getStep(-5)).isEqualTo(new Vec2(0, 0));
        assertThat(path.getStep(99)).isEqualTo(new Vec2(0, 0));
    }

    @Test
    void getStepReturnsExactlyThePixelPointThatWasAdded() {
        PathNormal path = new PathNormal(SCALE);
        // far outside the 1x1 grid below, so finalise() never dereferences its one
        // (deliberately unpopulated) cell - see RecordingCell
        path.addStep(500, 500);
        path.finalise(new RecordingCell[1][1]);

        assertThat(path.getStep(0)).isEqualTo(new Vec2(500, 500));
    }

    @Test
    void negativeStepClampsToFirstStep() {
        PathNormal path = new PathNormal(SCALE);
        path.addStep(500, 500);
        path.addStep(600, 600);
        path.finalise(new RecordingCell[1][1]);

        assertThat(path.getStep(-1)).isEqualTo(path.getStep(0));
    }

    @Test
    void stepPastEndClampsToLastStep() {
        PathNormal path = new PathNormal(SCALE);
        path.addStep(500, 500);
        path.addStep(600, 600);
        path.finalise(new RecordingCell[1][1]);

        assertThat(path.getStep(99)).isEqualTo(path.getStep(1));
    }

    @Test
    void lengthReflectsAddedStepsAfterFinalise() {
        PathNormal path = new PathNormal(SCALE);
        path.addStep(500, 500);
        path.addStep(600, 600);
        path.addStep(700, 700);
        path.finalise(new RecordingCell[1][1]);

        assertThat(path.length()).isEqualTo(3);
    }

    @Test
    void finaliseDisablesEveryCellTheStraightPathCoversAndLeavesOthersUntouched() {
        // a straight horizontal path from cell (0,1) to cell (2,1): pixel (5,15) to (25,15) at
        // this scale - covers every cell it crosses, not just its two listed endpoints
        PathNormal path = new PathNormal(SCALE);
        path.addStep(5, 15);
        path.addStep(25, 15);

        RecordingCell[][] grid = new RecordingCell[3][3];
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                grid[x][y] = new RecordingCell();
            }
        }
        path.finalise(grid);

        assertThat(grid[0][1].wasDisabled()).isTrue();
        assertThat(grid[1][1].wasDisabled()).isTrue();
        assertThat(grid[2][1].wasDisabled()).isTrue();
        assertThat(grid[0][0].enableWasCalled()).isFalse();
        assertThat(grid[2][2].enableWasCalled()).isFalse();
    }

    @Test
    void finaliseNeverTouchesACellFarFromThePath() {
        PathNormal path = new PathNormal(SCALE);
        path.addStep(500, 500);
        path.addStep(600, 600);

        // grid[0][0] is deliberately left null (RecordingCell[1][1] is an array of nulls) -
        // finalise() must never dereference it, since even after the path's bounding box is
        // clamped into this grid's only cell, the real distance to it is still far too large
        // to count as covered
        path.finalise(new RecordingCell[1][1]);

        assertThat(path.length()).isEqualTo(2);
    }
}
