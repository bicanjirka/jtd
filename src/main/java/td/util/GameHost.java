package td.util;

public interface GameHost {
    void enemyDied(int enemiesLeft);

    void setInfoText(String s);

    void clearCell(int x, int y);

    /**
     * A real, safe substitute for a display-less world (e.g. a toolbar's preview towers, or
     * a wave-preview panel's off-board enemies) - answers every callback with a no-op instead
     * of forcing a caller to pass {@code null} and hope nothing ever calls through it.
     */
    static GameHost noOp() {
        return new GameHost() {
            @Override
            public void enemyDied(int enemiesLeft) {
            }

            @Override
            public void setInfoText(String s) {
            }

            @Override
            public void clearCell(int x, int y) {
            }
        };
    }
}
