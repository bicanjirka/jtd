package td.level;

import org.junit.jupiter.api.Test;
import td.enemy.BodyArchetype;
import td.enemy.EnemyDefinition;
import td.enemy.Rank;
import td.wave.PathDefinition;
import td.wave.Point;
import td.wave.WaveDefinition;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LevelDefinitionTest {

    private static PathDefinition pathWithRounds(int roundCount) {
        List<WaveDefinition> waves = new ArrayList<>();
        for (int i = 0; i < roundCount; i++) {
            waves.add(new WaveDefinition("c", 1, 1, Rank.GRUNT));
        }
        return PathDefinition.of(List.of(new Point(0, 0), new Point(1, 0)), waves);
    }

    @Test
    void mutatingTheListPassedInDoesNotChangeTheStoredPaths() {
        List<PathDefinition> paths = new ArrayList<>(List.of(pathWithRounds(1)));
        LevelDefinition level = new LevelDefinition("Test", "", 5, 5, paths, List.of(), 100, 5);

        paths.add(pathWithRounds(1));

        assertThat(level.paths()).hasSize(1);
    }

    @Test
    void theStoredPathsCannotBeMutatedThroughTheirAccessor() {
        LevelDefinition level = new LevelDefinition("Test", "", 5, 5, List.of(pathWithRounds(1)), List.of(), 100, 5);

        assertThatThrownBy(() -> level.paths().add(pathWithRounds(1)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void aLevelNeedsAtLeastOnePath() {
        assertThatThrownBy(() -> new LevelDefinition("Test", "", 5, 5, List.of(), List.of(), 100, 5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void everyPathMustDefineTheSameNumberOfWaves() {
        assertThatThrownBy(() -> new LevelDefinition("Test", "", 5, 5,
                List.of(pathWithRounds(3), pathWithRounds(2)), List.of(), 100, 5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pathsWithMatchingRoundCountsAreAccepted() {
        LevelDefinition level = new LevelDefinition("Test", "", 5, 5,
                List.of(pathWithRounds(3), pathWithRounds(3)), List.of(), 100, 5);

        assertThat(level.paths()).hasSize(2);
    }

    @Test
    void unsmoothedBuildsASinglePathLevelWithNoSmoothing() {
        LevelDefinition level = LevelDefinition.unsmoothed("Test", "", 5, 5,
                List.of(new Point(0, 0), new Point(1, 0)), List.of(new WaveDefinition("c", 1, 1, Rank.GRUNT)), 100, 5);

        assertThat(level.paths()).hasSize(1);
        assertThat(level.paths().getFirst().corners()).containsExactly(new Point(0, 0), new Point(1, 0));
        assertThat(level.paths().getFirst().waves()).containsExactly(new WaveDefinition("c", 1, 1, Rank.GRUNT));
    }

    @Test
    void aLevelHasNoCustomEnemiesUnlessItAsksForSome() {
        LevelDefinition level = LevelDefinition.of("Test", 5, 5, List.of(pathWithRounds(1)));

        assertThat(level.customEnemies()).isEmpty();
    }

    @Test
    void mutatingTheListPassedInDoesNotChangeTheStoredCustomEnemies() {
        EnemyDefinition tankySquare = EnemyDefinition.of("tankySquare", "Tanky Square", 100, 5, 1.28f,
                BodyArchetype.SQUARE);
        List<EnemyDefinition> customEnemies = new ArrayList<>(List.of(tankySquare));
        LevelDefinition level = LevelDefinition.of("Test", 5, 5, List.of(pathWithRounds(1)))
                .withCustomEnemies(customEnemies);

        customEnemies.add(tankySquare);

        assertThat(level.customEnemies()).containsExactly(tankySquare);
    }

    @Test
    void theStoredCustomEnemiesCannotBeMutatedThroughTheirAccessor() {
        EnemyDefinition tankySquare = EnemyDefinition.of("tankySquare", "Tanky Square", 100, 5, 1.28f,
                BodyArchetype.SQUARE);
        LevelDefinition level = LevelDefinition.of("Test", 5, 5, List.of(pathWithRounds(1)))
                .withCustomEnemies(List.of(tankySquare));

        assertThatThrownBy(() -> level.customEnemies().add(tankySquare))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
