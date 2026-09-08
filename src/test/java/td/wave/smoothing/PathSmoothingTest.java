package td.wave.smoothing;

import org.junit.jupiter.api.Test;
import td.wave.Vec2;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PathSmoothingTest {

    @Test
    void noneReturnsThePolylineUnchanged() {
        List<Vec2> polyline = List.of(new Vec2(0, 0), new Vec2(10, 0), new Vec2(10, 10));

        assertThat(PathSmoothing.none().smooth(polyline)).isEqualTo(polyline);
    }

    @Test
    void noneDoesNotExposeTheOriginalListForMutation() {
        List<Vec2> polyline = List.of(new Vec2(0, 0), new Vec2(10, 0));

        assertThat(PathSmoothing.none().smooth(polyline)).isUnmodifiable();
    }
}
