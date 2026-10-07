package td.fixtures;

import td.board.BoardGeometry;
import td.util.GameWorld;
import td.util.RandomSource;
import td.util.RecordingGameHost;

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
}
