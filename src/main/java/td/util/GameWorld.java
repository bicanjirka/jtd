package td.util;

import td.board.BoardGeometry;
import td.economy.EconomyDelta;
import td.economy.EconomyLedger;
import td.economy.EconomyListener;
import td.enemy.EnemyCatalog;
import td.enemy.EnemyMob;
import td.enemy.EnemyRegistry;
import td.enemy.EnemyRoster;
import td.projectile.Projectile;
import td.projectile.ProjectileRegistry;
import td.projectile.ProjectileRoster;
import td.tower.Tower;
import td.tower.TowerListener;
import td.tower.TowerRoster;
import td.wave.Path;
import td.wave.PathNormal;
import td.wave.Wave;
import td.wave.WaveAnnouncer;
import td.wave.WaveStartListener;

import java.util.List;

/**
 * The composition root wiring a level's board geometry, economy, enemy roster, tower
 * roster, projectile roster and wave-start hub into the one object
 * {@code Tower}/{@code EnemyMob}/{@code Wave} are constructed against. Owns none of that
 * state itself - every method here delegates to {@link BoardGeometry}, {@link EconomyLedger},
 * {@link EnemyRoster}, {@link TowerRoster}, {@link ProjectileRoster} or {@link WaveAnnouncer},
 * each of which is independently constructible and testable.
 */
public class GameWorld {

    // Level-scoped state GameWorld owns directly rather than delegating. All three are
    // immutable values replaced wholesale when a level loads (on the EDT) and read every tick
    // (on the game-loop thread), so they are published volatile. See CLAUDE.md 3.
    private volatile BoardGeometry board = BoardGeometry.empty();
    private volatile Path path;
    private volatile EnemyCatalog enemyCatalog = EnemyCatalog.builtIn();
    private final GameHost mainApp;

    private final RandomSource random;
    private final EconomyLedger economy = new EconomyLedger();
    private final EnemyRoster enemies;
    private final TowerRoster towers;
    private final ProjectileRoster projectiles = new ProjectileRoster();
    private final WaveAnnouncer waves = new WaveAnnouncer();

    public GameWorld(GameHost mainApp) {
        this(mainApp, RandomSource.shared());
    }

    /** For a run that has to be reproducible - see {@code td.BalanceHarness}. */
    public GameWorld(GameHost mainApp, RandomSource random) {
        this.random = random;
        this.mainApp = mainApp;
        this.enemies = new EnemyRoster(mainApp);
        this.towers = new TowerRoster(mainApp, this.economy, this::getBoard);
        this.path = new PathNormal(List.of());
    }

    public void startWave(Wave w) {
        this.setEnemyCount(w.enemyCount());
        this.waves.announce();
    }

    public void addWaveStartListener(WaveStartListener l) {
        this.waves.addListener(l);
    }

    public void removeWaveStartListener(WaveStartListener l) {
        this.waves.removeListener(l);
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

    public void clearEnemies() {
        this.enemies.clear();
    }

    public EnemyCatalog getEnemyCatalog() {
        return this.enemyCatalog;
    }

    public void setEnemyCatalog(EnemyCatalog enemyCatalog) {
        this.enemyCatalog = enemyCatalog;
    }

    /** Adds an enemy outside a wave's own spawn sequence - an ability's reinforcement or egg spawn. */
    public void addEnemy(EnemyMob mob) {
        this.enemies.add(mob);
    }

    /** Replaces one live enemy with another as one step - an ability's egg hatch, not a kill. See {@code EnemyRoster.replace}. */
    public void replaceEnemy(EnemyMob outgoing, EnemyMob incoming) {
        this.enemies.replace(outgoing, incoming);
    }

    /** Where anything in the simulation that needs randomness gets it - never {@code Math.random()}. */
    public RandomSource random() {
        return this.random;
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

    public ProjectileRegistry getProjectileRegistry() {
        return this.projectiles;
    }

    public void addProjectile(Projectile projectile) {
        this.projectiles.add(projectile);
    }

    public void tickProjectiles(int gameTime) {
        this.projectiles.doTick(gameTime);
    }

    public void clearProjectiles() {
        this.projectiles.clear();
    }

}
