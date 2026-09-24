package td.util;

public interface GameHost {
    /**
     * A host that ignores every callback, for worlds with no display such as previews. Use instead
     * of {@code null}.
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

    void enemyDied(int enemiesLeft);

    void setInfoText(String s);

    void clearCell(int x, int y);
}
