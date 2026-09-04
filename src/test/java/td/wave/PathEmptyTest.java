package td.wave;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PathEmptyTest {

    @Test
    void lengthIsAlwaysOne() {
        PathEmpty path = new PathEmpty();

        assertThat(path.length()).isEqualTo(1);

        path.addStep(3, 4);
        assertThat(path.length()).isEqualTo(1);
    }

    @Test
    void getStepAlwaysReturnsTheLastAddedCoordinateRegardlessOfStepArgument() {
        PathEmpty path = new PathEmpty();
        path.addStep(7, 9);

        assertThat(path.getStep(0)).isEqualTo(new Point(7, 9));
        assertThat(path.getStep(-3)).isEqualTo(new Point(7, 9));
        assertThat(path.getStep(1000)).isEqualTo(new Point(7, 9));
    }

    @Test
    void defaultsToOriginBeforeAnyStepIsAdded() {
        PathEmpty path = new PathEmpty();

        assertThat(path.getStep(0)).isEqualTo(new Point(0, 0));
    }

    @Test
    void finaliseIsANoOp() {
        PathEmpty path = new PathEmpty();
        path.addStep(1, 2);

        // must not throw even with a null grid, since PathEmpty ignores it
        path.finalise(null);

        assertThat(path.getStep(0)).isEqualTo(new Point(1, 2));
    }
}
