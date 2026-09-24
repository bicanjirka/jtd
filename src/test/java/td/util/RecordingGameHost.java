package td.util;

/** A {@link GameHost} that records what was called. */
public class RecordingGameHost implements GameHost {

    public String lastInfoText;
    public int[] lastClearedCell;

    @Override
    public void setInfoText(String s) {
        this.lastInfoText = s;
    }

    @Override
    public void clearCell(int x, int y) {
        this.lastClearedCell = new int[]{x, y};
    }
}
