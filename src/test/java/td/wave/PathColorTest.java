package td.wave;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PathColorTest {

    @Test
    void ofStoresTheGivenComponents() {
        PathColor color = PathColor.of(10, 20, 30);

        assertThat(color.r()).isEqualTo(10);
        assertThat(color.g()).isEqualTo(20);
        assertThat(color.b()).isEqualTo(30);
    }

    @Test
    void componentsAreClampedIntoTheZeroTo255Range() {
        PathColor tooLow = PathColor.of(-10, -1, 0);
        PathColor tooHigh = PathColor.of(300, 256, 255);

        assertThat(tooLow).isEqualTo(PathColor.of(0, 0, 0));
        assertThat(tooHigh).isEqualTo(PathColor.of(255, 255, 255));
    }

    @Test
    void defaultIsWhite() {
        assertThat(PathColor.DEFAULT).isEqualTo(PathColor.of(255, 255, 255));
    }
}
