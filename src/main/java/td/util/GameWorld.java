package td.util;

import td.board.BoardGeometry;
import td.cell.CellGrid;
import td.economy.EconomyLedger;
import td.enemy.EnemyCatalog;
import td.enemy.EnemyRoster;
import td.projectile.ProjectileRoster;
import td.tower.TowerRoster;
import td.wave.Path;
import td.wave.Wave;
import td.wave.WaveAnnouncer;

import java.util.List;

/**
 * Wires the economy, rosters and wave-start hub that towers, enemies and waves are built against.
 * <p>
 * It hands out its collaborators rather than wrapping them: call {@link #economy()} or
 * {@link #towers()}, so each call site shows what it uses.
 * <p>
 * Its own state is the installed {@link LoadedLevel}, published through one {@code volatile}, and
 * the {@link RandomSource}.
 */
public class GameWorld {

    private final GameHost mainApp;
    private final RandomSource random;
    private final EconomyLedger economy = new EconomyLedger();
    private final EnemyRoster enemies;
    private final TowerRoster towers;
    private final ProjectileRoster projectiles = new ProjectileRoster();
    private final WaveAnnouncer waves = new WaveAnnouncer();
    private volatile LoadedLevel level = LoadedLevel.none();

    public GameWorld(GameHost mainApp) {
        this(mainApp, RandomSource.shared());
    }

    /** For a run that has to be reproducible. */
    public GameWorld(GameHost mainApp, RandomSource random) {
        this.random = random;
        this.mainApp = mainApp;
        this.enemies = new EnemyRoster(mainApp);
        this.towers = new TowerRoster(mainApp, this.economy, this::getBoard);
    }

    public EconomyLedger economy() {
        return this.economy;
    }

    public EnemyRoster enemies() {
        return this.enemies;
    }

    public TowerRoster towers() {
        return this.towers;
    }

    public ProjectileRoster projectiles() {
        return this.projectiles;
    }

    public WaveAnnouncer waves() {
        return this.waves;
    }

    /** The only source of randomness in the simulation. */
    public RandomSource random() {
        return this.random;
    }

    /**
     * Seeds the roster's alive count with the total across every path's starting wave, then
     * announces the start - in that order, so listeners never see a stale count.
     */
    public void startWave(List<Wave> starting) {
        int total = 0;
        for (Wave w : starting) {
            total += w.enemyCount();
        }
        this.enemies.setCount(total);
        this.waves.announce();
    }

    /**
     * The installed level as one snapshot. <strong>Read it once when you need two of its
     * parts</strong>: separate getters can straddle a level change.
     */
    public LoadedLevel level() {
        return this.level;
    }

    /** Installs a level in one write. */
    public void installLevel(LoadedLevel level) {
        this.level = level;
    }

    /** {@link CellGrid#empty()} when no level is installed. */
    public CellGrid cells() {
        return this.level.cells();
    }

    public BoardGeometry getBoard() {
        return this.level.board();
    }

    /**
     * Replaces one part of the installed level. <strong>Single-threaded worlds only</strong>
     * (tests, previews): it is a read-modify-write. The simulation installs levels with
     * {@link #installLevel}.
     */
    public void setBoard(BoardGeometry board) {
        this.level = this.level.withBoard(board);
    }

    public Path getPath() {
        return this.level.pathAt(0);
    }

    /** Single-threaded worlds only; see {@link #setBoard}. */
    public void setPath(Path path) {
        this.level = this.level.withSinglePath(path);
    }

    public EnemyCatalog getEnemyCatalog() {
        return this.level.catalog();
    }

    /** Single-threaded worlds only; see {@link #setBoard}. */
    public void setEnemyCatalog(EnemyCatalog enemyCatalog) {
        this.level = this.level.withCatalog(enemyCatalog);
    }

    public void setInfoText(String s) {
        this.mainApp.setInfoText(s);
    }

}
