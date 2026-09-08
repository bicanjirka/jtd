package td.level;

import org.junit.jupiter.api.Test;
import td.wave.Point;
import td.wave.WaveDefinition;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LevelDefinitionTest {

    @Test
    void mutatingTheListPassedInDoesNotChangeTheStoredPath() {
        List<Point> path = new ArrayList<>(List.of(new Point(0, 0), new Point(1, 0)));
        LevelDefinition level = new LevelDefinition("Test", "", 5, 5, path, List.of(), 100, 5);

        path.add(new Point(2, 0));

        assertThat(level.path()).containsExactly(new Point(0, 0), new Point(1, 0));
    }

    @Test
    void mutatingTheListPassedInDoesNotChangeTheStoredWaves() {
        List<WaveDefinition> waves = new ArrayList<>(List.of(new WaveDefinition("c", 1, 1, 1)));
        LevelDefinition level = new LevelDefinition("Test", "", 5, 5, List.of(), waves, 100, 5);

        waves.add(new WaveDefinition("s", 1, 1, 1));

        assertThat(level.waves()).containsExactly(new WaveDefinition("c", 1, 1, 1));
    }

    @Test
    void theStoredPathAndWavesCannotBeMutatedThroughTheirAccessors() {
        LevelDefinition level = new LevelDefinition("Test", "", 5, 5,
                List.of(new Point(0, 0)), List.of(new WaveDefinition("c", 1, 1, 1)), 100, 5);

        assertThatThrownBy(() -> level.path().add(new Point(1, 0)))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> level.waves().add(new WaveDefinition("s", 1, 1, 1)))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
