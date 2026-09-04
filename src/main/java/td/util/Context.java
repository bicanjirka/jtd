package td.util;

import td.enemy.EnemyMob;
import td.tower.Tower;
import td.wave.Path;
import td.wave.PathNormal;
import td.wave.Wave;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Context {

    public int scale = 32;
    public int maxX, maxY;
    public EnemyMob[] enemies;
    public final List<Tower> towers;
    private final GameHost mainApp;
    private int enemyCount = 0;
    private Path path;

    private final int livesMax = 5;
    private int lives = 5;
    private int credits = 0;
    private int score = 0;

    private final List<ContextListener> contextListeners;
    private final List<TowerListener> towerListeners;
    private final List<WaveStartListener> waveListeners;

    private final Cache cache;

    public Context(GameHost mainApp) {
        this.mainApp = mainApp;
        this.contextListeners = new CopyOnWriteArrayList<>();
        this.towerListeners = new CopyOnWriteArrayList<>();
        this.waveListeners = new CopyOnWriteArrayList<>();
        this.towers = new CopyOnWriteArrayList<>();
        this.path = new PathNormal(this.scale);
        this.cache = Cache.getInstance();
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

    public void setEnemyCount(int c) {
        this.enemyCount = c;
    }

    public void removeEnemy() {
        this.enemyCount--;
        this.mainApp.enemyDied(this.enemyCount);
    }

    public void removeAllEnemies() {
        this.enemyCount = 0;
        this.enemies = new EnemyMob[0];
        this.mainApp.enemyDied(this.enemyCount);
    }

    public void setInfoText(String s) {
        this.mainApp.setInfoText(s);
    }

    public void addScore(int s) {
        if (s < 0) {
            System.out.println("Context::addScore: Adding negative score? " + s);
        }
        this.score += s;
    }

    public void deductScore(int s) {
        if (s < 0) {
            System.out.println("Context::deductScore: Deducting negative score? " + s);
        }
        this.score -= s;
    }

    public int getScore() {
        return this.score;
    }

    public void resetScore() {
        this.score = 0;
    }

    public int getCredits() {
        return this.credits;
    }

    public void setCredits(int credits) {
        this.credits = credits;
        this.fireMoneyChangedEvent();
    }

    public boolean canPay(int amount) {
        return (this.credits >= amount);
    }

    public boolean doPay(int amount) {
        if (this.canPay(amount)) {
            this.credits -= amount;
            this.fireMoneyChangedEvent();
            return true;
        } else {
            return false;
        }
    }

    public void doReceive(int amount) {
        this.credits += amount;
        this.fireMoneyChangedEvent();
    }

    public void addTower(Tower t) {
        this.towers.add(t);
        this.fireTowerAddedEvent(t);
    }

    public void sellTower(Tower t) {
        int cellX = t.getX() / this.scale;
        int cellY = t.getY() / this.scale;
        this.mainApp.clearCell(cellX, cellY);
        t.doCleanup();
        this.towers.remove(t);
        this.doReceive(t.getSellPrice());
        this.fireTowerRemovedEvent(t);
    }

    public void clearTowers() {
        for (Tower t : this.towers) {
            int cellX = t.getX() / this.scale;
            int cellY = t.getY() / this.scale;
            this.mainApp.clearCell(cellX, cellY);
            t.doCleanup();
            this.towers.remove(t);
            this.fireTowerRemovedEvent(t);
        }
    }

    public void addTowerListener(TowerListener l) {
        this.towerListeners.add(l);
    }

    public void removeTowerListener(TowerListener l) {
        this.towerListeners.remove(l);
    }

    private void fireTowerAddedEvent(Tower t) {
        for (TowerListener l : this.towerListeners) {
            l.towerBuild(t);
        }
    }

    private void fireTowerRemovedEvent(Tower t) {
        for (TowerListener l : this.towerListeners) {
            l.towerRemoved(t);
        }
    }

    public void addContextListener(ContextListener l) {
        this.contextListeners.add(l);
    }

    public void removeContextListener(ContextListener l) {
        this.contextListeners.remove(l);
    }

    private void fireMoneyChangedEvent() {
        for (ContextListener l : this.contextListeners) {
            l.moneyChanged();
        }
    }

    private void fireLivesChangedEvent() {
        for (ContextListener l : this.contextListeners) {
            l.livesChanged();
        }
    }

    public Path getPath() {
        return this.path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public int getLives() {
        return lives;
    }

    public void removeLife() {
        this.lives--;
        this.fireLivesChangedEvent();
    }

    public void resetLives() {
        this.lives = this.livesMax;
        this.fireLivesChangedEvent();
    }

    public Cache getCache() {
        return this.cache;
    }

}
