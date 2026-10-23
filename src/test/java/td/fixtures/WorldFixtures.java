package td.fixtures;

import td.board.BoardGeometry;
import td.util.GameWorld;
import td.util.RandomSource;
import td.util.RecordingGameHost;
import td.wave.PathNormal;
import td.wave.Vec2;

import java.util.List;

/**
 * A real headless {@link GameWorld} with a {@link RecordingGameHost}. Pass your own host to
 * {@link #newWorld(RecordingGameHost)} when you need to inspect it afterwards.
 */
public final class WorldFixtures {

    private WorldFixtures() {
    }

    public static GameWorld newWorld() {
        return new GameWorld(new RecordingGameHost());
    }

    public static GameWorld newWorld(RecordingGameHost host) {
        return new GameWorld(host);
    }

    /** For a test that controls a roll. */
    public static GameWorld newWorld(RandomSource random) {
        return new GameWorld(new RecordingGameHost(), random);
    }

    /** A world with a board installed. */
    public static GameWorld newWorldOnBoard(int scale, int width, int height) {
        GameWorld world = newWorld();
        world.setBoard(BoardGeometry.of(scale, width, height));
        return world;
    }

    /** A world with a board installed, for a test that controls a roll. */
    public static GameWorld newWorldOnBoard(RandomSource random, int scale, int width, int height) {
        GameWorld world = newWorld(random);
        world.setBoard(BoardGeometry.of(scale, width, height));
        return world;
    }

    /** A world with a board installed, on the caller's host. */
    public static GameWorld newWorldOnBoard(RecordingGameHost host, int scale, int width, int height) {
        GameWorld world = newWorld(host);
        world.setBoard(BoardGeometry.of(scale, width, height));
        return world;
    }

    /** A world whose one path runs straight along the top edge, long enough that nothing leaks. */
    public static GameWorld newWorldOnStraightPath() {
        return onStraightPath(newWorldOnBoard(BoardFixtures.SCALE, 1000, 1000));
    }

    /** {@link #newWorldOnStraightPath()} for a test that controls a roll. */
    public static GameWorld newWorldOnStraightPath(RandomSource random) {
        return onStraightPath(newWorldOnBoard(random, BoardFixtures.SCALE, 1000, 1000));
    }

    private static GameWorld onStraightPath(GameWorld world) {
        world.setPath(new PathNormal(List.of(new Vec2(0, 0), new Vec2(1000, 0))));
        return world;
    }
}
