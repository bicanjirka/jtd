package td.util;

public interface GameHost {
    void enemyDied(int enemiesLeft);

    void setInfoText(String s);

    void clearCell(int x, int y);
}
