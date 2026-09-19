package td.wave;

import org.junit.jupiter.api.Test;
import td.enemy.Rank;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PathDefinitionTest {

    @Test
    void mutatingTheListPassedInDoesNotChangeTheStoredCorners() {
        List<Point> corners = new ArrayList<>(List.of(new Point(0, 0), new Point(1, 0)));
        PathDefinition path = PathDefinition.of(corners, List.of());

        corners.add(new Point(2, 0));

        assertThat(path.corners()).containsExactly(new Point(0, 0), new Point(1, 0));
    }

    @Test
    void mutatingTheListPassedInDoesNotChangeTheStoredWaves() {
        List<WaveDefinition> waves = new ArrayList<>(List.of(new WaveDefinition("c", 1, 1, Rank.GRUNT)));
        PathDefinition path = PathDefinition.of(List.of(new Point(0, 0), new Point(1, 0)), waves);

        waves.add(new WaveDefinition("s", 1, 1, Rank.GRUNT));

        assertThat(path.waves()).containsExactly(new WaveDefinition("c", 1, 1, Rank.GRUNT));
    }

    @Test
    void theStoredCornersAndWavesCannotBeMutatedThroughTheirAccessors() {
        PathDefinition path = PathDefinition.of(
                List.of(new Point(0, 0), new Point(1, 0)), List.of(new WaveDefinition("c", 1, 1, Rank.GRUNT)));

        assertThatThrownBy(() -> path.corners().add(new Point(2, 0)))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> path.waves().add(new WaveDefinition("s", 1, 1, Rank.GRUNT)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void aPathNeedsAtLeastTwoCorners() {
        assertThatThrownBy(() -> PathDefinition.of(List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> PathDefinition.of(List.of(new Point(0, 0)), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ofDefaultsToNoSmoothingDefaultColorAndNormalSpeed() {
        PathDefinition path = PathDefinition.of(List.of(new Point(0, 0), new Point(1, 0)), List.of());

        assertThat(path.color()).isEqualTo(PathColor.DEFAULT);
        assertThat(path.speedMultiplier()).isEqualTo(1f);
    }

    @Test
    void withColorAndWithSpeedReturnCopiesLeavingTheOriginalUnchanged() {
        PathDefinition original = PathDefinition.of(List.of(new Point(0, 0), new Point(1, 0)), List.of());

        PathDefinition recolored = original.withColor(PathColor.of(10, 20, 30));
        PathDefinition sped = original.withSpeed(1.5f);

        assertThat(original.color()).isEqualTo(PathColor.DEFAULT);
        assertThat(original.speedMultiplier()).isEqualTo(1f);
        assertThat(recolored.color()).isEqualTo(PathColor.of(10, 20, 30));
        assertThat(recolored.speedMultiplier()).isEqualTo(1f);
        assertThat(sped.color()).isEqualTo(PathColor.DEFAULT);
        assertThat(sped.speedMultiplier()).isEqualTo(1.5f);
    }

    @Test
    void aNonPositiveSpeedMultiplierIsRejected() {
        PathDefinition path = PathDefinition.of(List.of(new Point(0, 0), new Point(1, 0)), List.of());

        assertThatThrownBy(() -> path.withSpeed(0f)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> path.withSpeed(-1f)).isInstanceOf(IllegalArgumentException.class);
    }
}
