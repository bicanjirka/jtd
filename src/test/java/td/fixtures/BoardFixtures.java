package td.fixtures;

/**
 * The board scale every test that doesn't care about a specific value uses, and the one place
 * "cell index to pixel center" is computed - so a test names a cell, not a pixel offset it has
 * to get right by hand.
 */
public final class BoardFixtures {

    public static final int SCALE = 32;

    private BoardFixtures() {
    }

    public static int cellCenter(int cellIndex) {
        return cellIndex * SCALE + SCALE / 2;
    }
}
