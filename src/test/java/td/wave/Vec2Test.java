package td.wave;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Vec2Test {

    @Test
    void pointsWithSameCoordinatesAreEqual() {
        assertThat(new Vec2(3.0, 4.0)).isEqualTo(new Vec2(3.0, 4.0));
        assertThat(new Vec2(3.0, 4.0)).isNotEqualTo(new Vec2(4.0, 3.0));
    }

    @Test
    void accessorsReturnConstructorArguments() {
        Vec2 v = new Vec2(5.5, -2.25);

        assertThat(v.x()).isEqualTo(5.5);
        assertThat(v.y()).isEqualTo(-2.25);
    }
}
