package td.wave;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PathNormalTest {

    @Test
    void pointsReturnsExactlyWhatWasGiven() {
        PathNormal path = new PathNormal(List.of(new Vec2(5, 5), new Vec2(15, 5)));

        assertThat(path.points()).containsExactly(new Vec2(5, 5), new Vec2(15, 5));
    }

    @Test
    void anEmptyPathHasNoPoints() {
        PathNormal path = new PathNormal(List.of());

        assertThat(path.points()).isEmpty();
    }

    @Test
    void mutatingTheListPassedInDoesNotChangeTheStoredPoints() {
        List<Vec2> points = new ArrayList<>(List.of(new Vec2(0, 0), new Vec2(10, 0)));
        PathNormal path = new PathNormal(points);

        points.add(new Vec2(20, 0));

        assertThat(path.points()).containsExactly(new Vec2(0, 0), new Vec2(10, 0));
    }

    @Test
    void theStoredPointsCannotBeMutatedThroughTheAccessor() {
        PathNormal path = new PathNormal(List.of(new Vec2(0, 0)));

        assertThatThrownBy(() -> path.points().add(new Vec2(1, 1)))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
