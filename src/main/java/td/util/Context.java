package td.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import td.economy.EconomyDelta;
import td.economy.EconomyState;
import td.enemy.EnemyMob;
import td.tower.Tower;
import td.wave.Path;
import td.wave.PathNormal;
import td.wave.Wave;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Context {

    private static final Logger LOG = LoggerFactory.getLogger(Context.class);

    public int scale = 32;
    public int maxX, maxY;
    private EnemyMob[] enemies = new EnemyMob[0];
    public final List<Tower> towers;
    private final GameHost mainApp;
    private int enemyCount = 0;
    private Path path;

    private volatile EconomyState economy = EconomyState.startingWith(0, 5);

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
        this.path = new PathNormal(List.of());
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

    public EnemyMob[] getEnemies() {
        return this.enemies;
    }

    public void setEnemies(EnemyMob[] enemies) {
        this.enemies = enemies;
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

    /**
     * Seeds the economy at the start of a level - credits and lives are both the level's own,
     * not carried over from whatever ran before. Fires like any other economy change since
     * listeners are already registered by the time a level loads.
     */
    public void startEconomy(int startingCredits, int startingLives) {
        this.economy = EconomyState.startingWith(startingCredits, startingLives);
        LOG.debug("Economy seeded: {}", this.economy);
        this.fireEconomyChangedEvent(this.economy);
    }

    /**
     * Applies a game event's effect on credits/score/lives as one atomic move, firing exactly
     * one notification for it - replacing what used to be up to three separate mutations
     * (see EconomyDelta.kill/leak).
     */
    public void apply(EconomyDelta delta) {
        EconomyState updated;
        synchronized (this) {
            updated = this.economy.after(delta);
            this.economy = updated;
        }
        LOG.debug("Economy {} -> {}", delta, updated);
        this.fireEconomyChangedEvent(updated);
    }

    public int getScore() {
        return this.economy.score();
    }

    public int getCredits() {
        return this.economy.credits();
    }

    public boolean canPay(int amount) {
        return this.economy.canAfford(amount);
    }

    public boolean doPay(int amount) {
        EconomyState updated;
        synchronized (this) {
            if (!this.economy.canAfford(amount)) {
                return false;
            }
            updated = this.economy.after(EconomyDelta.credits(-amount));
            this.economy = updated;
        }
        LOG.debug("Credits -{} -> {}", amount, updated.credits());
        this.fireEconomyChangedEvent(updated);
        return true;
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
        this.apply(EconomyDelta.credits(t.getSellPrice()));
        this.fireTowerRemovedEvent(t);
        LOG.info("Tower sold: {} at ({},{}), refund={}", t.getType(), cellX, cellY, t.getSellPrice());
    }

    public void clearTowers() {
        for (Tower t : new ArrayList<>(this.towers)) {
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

    private void fireEconomyChangedEvent(EconomyState state) {
        for (ContextListener l : this.contextListeners) {
            l.economyChanged(state);
        }
    }

    public Path getPath() {
        return this.path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public int getLives() {
        return this.economy.lives();
    }

    public Cache getCache() {
        return this.cache;
    }

}
