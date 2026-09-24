package td.util;

/** A {@link GameHost} that records what was called. */
public class RecordingGameHost implements GameHost {

    public int[] lastClearedCell;

    @Override
    public void clearCell(int x, int y) {
        this.lastClearedCell = new int[]{x, y};
    }
}
