package td.util;

import java.util.ArrayList;
import java.util.List;

/** A {@link GameHost} that records what was called. */
public class RecordingGameHost implements GameHost {

    public final List<Integer> enemyDiedCalls = new ArrayList<>();
    public String lastInfoText;
    public int[] lastClearedCell;

    @Override
    public void enemyDied(int enemiesLeft) {
        this.enemyDiedCalls.add(enemiesLeft);
    }

    @Override
    public void setInfoText(String s) {
        this.lastInfoText = s;
    }

    @Override
    public void clearCell(int x, int y) {
        this.lastClearedCell = new int[]{x, y};
    }
}
