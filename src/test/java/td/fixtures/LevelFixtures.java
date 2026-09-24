package td.fixtures;

import td.level.LevelDefinition;
import td.wave.PathDefinition;
import td.wave.PathNormal;
import td.wave.Point;
import td.wave.Vec2;
import td.wave.WaveDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * Test levels shared across engine tests, so a change to {@link LevelDefinition} touches one file.
 */
public final class LevelFixtures {

    /** A straight path along row 2. */
    public static final List<Point> STRAIGHT_PATH = List.of(new Point(0, 2), new Point(4, 2));

    private LevelFixtures() {
    }

    /** A level with no waves. */
    public static LevelDefinition level(String name, int width, int height) {
        return LevelDefinition.unsmoothed(name, "", width, height, STRAIGHT_PATH, List.of(), 100, 5);
    }

    public static LevelDefinition levelWith(List<WaveDefinition> waves, int startingCredits) {
        return LevelDefinition.unsmoothed("Test Level", "", 5, 5, STRAIGHT_PATH, waves, startingCredits, 5);
    }

    public static LevelDefinition biggerLevelWith(List<WaveDefinition> waves, int startingCredits) {
        return LevelDefinition.unsmoothed("Bigger Level", "", 20, 15, STRAIGHT_PATH, waves, startingCredits, 5);
    }

    /**
     * Path A along row 2 and path B along row 5, far enough apart that no tower covering one
     * reaches the other.
     */
    public static LevelDefinition twoPathLevelWith(List<WaveDefinition> wavesA, List<WaveDefinition> wavesB) {
        return LevelDefinition.of("Two-Path Test Level", 5, 7,
                List.of(PathDefinition.of(STRAIGHT_PATH, wavesA),
                        PathDefinition.of(List.of(new Point(0, 5), new Point(4, 5)), wavesB)));
    }

    /** A straight pixel-space path along {@code y = scale/2} through the given cell columns. */
    public static PathNormal straightPath(int scale, int... xCoords) {
        List<Vec2> points = new ArrayList<>();
        for (int x : xCoords) {
            points.add(new Vec2(x * scale + (scale / 2), scale / 2));
        }
        return new PathNormal(points);
    }
}
