package td.level;

import org.junit.jupiter.api.Test;
import td.wave.Path;
import td.wave.PathBuilder;
import td.wave.PathDefinition;
import td.wave.Point;
import td.wave.Vec2;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BuiltInLevelCatalogTest {

    private static final int SCALE = 32;

    @Test
    void everyBuiltInLevelHasANameOfItsOwn() {
        List<String> names = new BuiltInLevelCatalog().levels().stream().map(LevelDefinition::name).toList();

        assertThat(names).isNotEmpty().doesNotHaveDuplicates().allSatisfy(name -> assertThat(name).isNotBlank());
    }

    /** A quadratic or arc corner never bulges past the corners it rounds, however far it pulls. */
    @Test
    void everyBuiltInPathsCurveStaysWithinItsOwnCorners() {
        for (LevelDefinition level : new BuiltInLevelCatalog().levels()) {
            for (PathDefinition path : level.paths()) {
                Path smoothedPath = PathBuilder.build(path.corners(), path.smoothing(), SCALE);
                int minX = path.corners().stream().mapToInt(Point::x).min().orElseThrow();
                int maxX = path.corners().stream().mapToInt(Point::x).max().orElseThrow();
                int minY = path.corners().stream().mapToInt(Point::y).min().orElseThrow();
                int maxY = path.corners().stream().mapToInt(Point::y).max().orElseThrow();

                for (Vec2 point : smoothedPath.points()) {
                    assertThat(point.x()).as(level.name()).isBetween((double) (minX * SCALE), (double) (maxX * SCALE + SCALE));
                    assertThat(point.y()).as(level.name()).isBetween((double) (minY * SCALE), (double) (maxY * SCALE + SCALE));
                }
            }
        }
    }
}
