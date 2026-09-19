package td.fixtures;

import td.board.BoardGeometry;
import td.util.GameWorld;
import td.util.RandomSource;
import td.util.RecordingGameHost;

/**
 * The {@link GameWorld} + {@link RecordingGameHost} pair almost every test in this suite builds
 * to get a real, headless simulation context - centralized so a change to either constructor
 * touches this one file instead of every test that builds one by hand.
 * <p>
 * A test that needs to inspect the host afterward (its {@code enemyDiedCalls}, its
 * {@code lastInfoText}) builds its own {@code RecordingGameHost} and passes it to
 * {@link #newWorld(RecordingGameHost)} rather than losing the reference inside this factory.
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

    /**
     * For a test that needs to control a roll (a crit chance, a random pick) rather than leaving
     * it to the shared generator.
     */
    public static GameWorld newWorld(RandomSource random) {
        return new GameWorld(new RecordingGameHost(), random);
    }

    /**
     * A world with a board already installed - the common case for a tower or placement test,
     * which needs somewhere to place cells before it can do anything else.
     */
    public static GameWorld newWorldOnBoard(int scale, int width, int height) {
        GameWorld world = newWorld();
        world.setBoard(BoardGeometry.of(scale, width, height));
        return world;
    }

    /**
     * A world with a board already installed, on a caller-supplied host - for a test that needs
     * to both set up the board and inspect the host afterward (its {@code enemyDiedCalls}).
     */
    public static GameWorld newWorldOnBoard(RecordingGameHost host, int scale, int width, int height) {
        GameWorld world = newWorld(host);
        world.setBoard(BoardGeometry.of(scale, width, height));
        return world;
    }
}
