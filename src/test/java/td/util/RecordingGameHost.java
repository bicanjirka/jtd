package td.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Test double for {@link GameHost} that just records what was called,
 * so tests can build a real {@link Context} without a live TowerDefence.
 */
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
