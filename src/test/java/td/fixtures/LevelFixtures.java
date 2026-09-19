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
 * The handful of test levels almost every engine-level test builds by hand - centralized so a
 * change to {@link LevelDefinition}'s shape touches this one file instead of every test that
 * builds one.
 */
public final class LevelFixtures {

    /** A straight path along row y=2 - also the waypoint {@code PathNormal.finalise()} marks unbuildable. */
    public static final List<Point> STRAIGHT_PATH = List.of(new Point(0, 2), new Point(4, 2));

    private LevelFixtures() {
    }

    /** A named level on a given board, with no waves - for tests that only care about its identity/size. */
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
     * A two-path level: path A repeats {@link #STRAIGHT_PATH} at row 2, path B is a separate
     * short straight path at row 5, on a taller board than {@link #levelWith} uses so the two
     * rows sit well apart - far enough that a tower covering path A cannot also reach path B.
     */
    public static LevelDefinition twoPathLevelWith(List<WaveDefinition> wavesA, List<WaveDefinition> wavesB) {
        return LevelDefinition.of("Two-Path Test Level", 5, 7,
                List.of(PathDefinition.of(STRAIGHT_PATH, wavesA),
                        PathDefinition.of(List.of(new Point(0, 5), new Point(4, 5)), wavesB)));
    }

    /**
     * A pixel-space straight path along {@code y = scale/2}, at the given cell-space x
     * coordinates - converted to pixel centers the same way production code ({@code PathBuilder})
     * does, since {@link PathNormal} stores pixel points directly.
     */
    public static PathNormal straightPath(int scale, int... xCoords) {
        List<Vec2> points = new ArrayList<>();
        for (int x : xCoords) {
            points.add(new Vec2(x * scale + (scale / 2), scale / 2));
        }
        return new PathNormal(points);
    }
}
