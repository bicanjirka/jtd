package td.fixtures;

/**
 * The default test board scale, and cell-to-pixel-centre conversion so tests name cells, not
 * pixels.
 */
public final class BoardFixtures {

    public static final int SCALE = 32;

    private BoardFixtures() {
    }

    public static int cellCenter(int cellIndex) {
        return cellIndex * SCALE + SCALE / 2;
    }
}
