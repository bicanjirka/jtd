package td.wave;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PointTest {

    @Test
    void pointsWithSameCoordinatesAreEqual() {
        assertThat(new Point(3, 4)).isEqualTo(new Point(3, 4));
        assertThat(new Point(3, 4)).isNotEqualTo(new Point(4, 3));
    }

    @Test
    void accessorsReturnConstructorArguments() {
        Point p = new Point(5, -2);

        assertThat(p.x()).isEqualTo(5);
        assertThat(p.y()).isEqualTo(-2);
    }
}
