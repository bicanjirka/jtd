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

    private static LevelDefinition twistedHourglass() {
        return new BuiltInLevelCatalog().levels().get(2);
    }

    @Test
    void theCatalogOffersAllThreeBuiltInLevelsInOrder() {
        List<LevelDefinition> levels = new BuiltInLevelCatalog().levels();

        assertThat(levels).extracting(LevelDefinition::name)
                .containsExactly("Curly Path", "Zigzag Path", "Twisted Hourglass");
    }

    @Test
    void twistedHourglassPathStaysEntirelyWithinItsOwnBoard() {
        LevelDefinition level = twistedHourglass();
        PathDefinition path = level.paths().getFirst();

        Path smoothedPath = PathBuilder.build(path.corners(), path.smoothing(), SCALE);

        int minX = path.corners().stream().mapToInt(Point::x).min().orElseThrow();
        int maxX = path.corners().stream().mapToInt(Point::x).max().orElseThrow();
        int minY = path.corners().stream().mapToInt(Point::y).min().orElseThrow();
        int maxY = path.corners().stream().mapToInt(Point::y).max().orElseThrow();

        // The convex-hull property of a quadratic Bezier guarantees the smoothed curve never
        // leaves the bounding box of its own raw corners, however large cornerPull is.
        for (Vec2 point : smoothedPath.points()) {
            assertThat(point.x()).isBetween((double) (minX * SCALE), (double) (maxX * SCALE + SCALE));
            assertThat(point.y()).isBetween((double) (minY * SCALE), (double) (maxY * SCALE + SCALE));
        }
    }
}
