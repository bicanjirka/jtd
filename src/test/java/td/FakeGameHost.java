package td;

import td.util.GameHost;

/**
 * A GameHost that actually forwards to a GameEngine, mirroring what
 * TowerDefence's real GameHost methods do (minus the Swing UI updates) -
 * so end-to-end tests exercise the same state transitions a real click
 * would trigger (e.g. selling a tower really clears the cell, a wave
 * completing really re-arms isWaveReady()).
 * <p>
 * Constructed in two steps because GameEngine's constructor needs a
 * GameHost before the GameEngine instance itself exists.
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
        // pure UI in TowerDefence's real implementation; nothing to replicate here
    }

    @Override
    public void clearCell(int x, int y) {
        engine.clearCell(x, y);
    }
}
