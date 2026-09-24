package td;

import td.util.GameHost;

/**
 * A {@link GameHost} that forwards to a real engine like the UI does, minus Swing, so tests see
 * real state transitions. Bound after construction, since the engine needs its host first.
 */
class FakeGameHost implements GameHost {

    private GameEngine engine;

    static GameEngine newBoundEngine() {
        FakeGameHost host = new FakeGameHost();
        GameEngine engine = new GameEngine(host);
        host.bind(engine);
        return engine;
    }

    void bind(GameEngine engine) {
        this.engine = engine;
    }

    @Override
    public void enemyDied(int enemiesLeft) {
        if (enemiesLeft == 0 && engine.getCurrentWaveIndex() < engine.getWaveCount()) {
            engine.setWaveReady(true);
        }
    }

    @Override
    public void setInfoText(String s) {
        // pure UI
    }

    @Override
    public void clearCell(int x, int y) {
        engine.clearCell(x, y);
    }
}
