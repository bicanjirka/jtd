package td.util;

import td.board.BoardGeometry;
import td.economy.EconomyDelta;
import td.economy.EconomyLedger;
import td.economy.EconomyListener;
import td.enemy.EnemyMob;
import td.enemy.EnemyRegistry;
import td.enemy.EnemyRoster;
import td.tower.Tower;
import td.tower.TowerListener;
import td.tower.TowerRoster;
import td.wave.Path;
import td.wave.PathNormal;
import td.wave.Wave;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Context {

    private BoardGeometry board = BoardGeometry.empty();
    private final GameHost mainApp;
    private Path path;

    private final EconomyLedger economy = new EconomyLedger();
    private final EnemyRoster enemies;
    private final TowerRoster towers;

    private final List<WaveStartListener> waveListeners;

    public Context(GameHost mainApp) {
        this.mainApp = mainApp;
        this.enemies = new EnemyRoster(mainApp);
        this.towers = new TowerRoster(mainApp, this.economy, this::getBoard);
        this.waveListeners = new CopyOnWriteArrayList<>();
        this.path = new PathNormal(List.of());
    }

    public void startWave(Wave w) {
        this.setEnemyCount(w.enemyCount());
        this.fireWaveStartedEvent();
    }

    public void addWaveStartListener(WaveStartListener l) {
        this.waveListeners.add(l);
    }

    public void removeWaveStartListener(WaveStartListener l) {
        this.waveListeners.remove(l);
    }

    private void fireWaveStartedEvent() {
        for (WaveStartListener l : this.waveListeners) {
            l.waveStarted();
        }
    }

    public EnemyRegistry getEnemyRegistry() {
        return this.enemies;
    }

    public void setEnemyCount(int c) {
        this.enemies.setCount(c);
    }

    public EnemyMob[] getEnemies() {
        return this.enemies.getEnemies();
    }

    public void setEnemies(EnemyMob[] enemies) {
        this.enemies.setEnemies(enemies);
    }

    public void removeEnemy() {
        this.enemies.remove();
    }

    public void removeAllEnemies() {
        this.enemies.removeAll();
    }

    public BoardGeometry getBoard() {
        return this.board;
    }

    public void setBoard(BoardGeometry board) {
        this.board = board;
    }

    public void setInfoText(String s) {
        this.mainApp.setInfoText(s);
    }

    public EconomyLedger getEconomy() {
        return this.economy;
    }

    public void startEconomy(int startingCredits, int startingLives) {
        this.economy.startEconomy(startingCredits, startingLives);
    }

    public void apply(EconomyDelta delta) {
        this.economy.apply(delta);
    }

    public int getScore() {
        return this.economy.getScore();
    }

    public int getCredits() {
        return this.economy.getCredits();
    }

    public boolean canPay(int amount) {
        return this.economy.canPay(amount);
    }

    public boolean doPay(int amount) {
        return this.economy.doPay(amount);
    }

    public List<Tower> getTowers() {
        return this.towers.all();
    }

    public void addTower(Tower t) {
        this.towers.add(t);
    }

    public void sellTower(Tower t) {
        this.towers.sell(t);
    }

    public void clearTowers() {
        this.towers.clear();
    }

    public void addTowerListener(TowerListener l) {
        this.towers.addListener(l);
    }

    public void removeTowerListener(TowerListener l) {
        this.towers.removeListener(l);
    }

    public void addEconomyListener(EconomyListener l) {
        this.economy.addEconomyListener(l);
    }

    public void removeEconomyListener(EconomyListener l) {
        this.economy.removeEconomyListener(l);
    }

    public Path getPath() {
        return this.path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public int getLives() {
        return this.economy.getLives();
    }

}
