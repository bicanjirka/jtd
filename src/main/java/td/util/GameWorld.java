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
 * The composition root wiring a level's economy, enemy roster, tower roster, projectile
 * roster and wave-start hub into the one object {@code Tower}/{@code EnemyMob}/{@code Wave}
 * are constructed against.
 * <p>
 * <strong>It hands its collaborators out; it does not wrap them.</strong> Ask for
 * {@link #economy()} or {@link #towers()} and call that. This used to be forty-one
 * delegating pass-throughs, which hid how much of the world any one class actually touched -
 * a tower looked like it used "the world" when it used seven specific capabilities. Naming
 * the collaborator at the call site makes that visible, and each collaborator is
 * independently constructible and testable on its own.
 * <p>
 * It is <em>not</em> a pure composition root. Two things are its own state rather than a
 * collaborator's: the {@link LoadedLevel} currently installed and the {@link RandomSource}.
 * The level is a correlated bundle - board, path, cell grid, enemy catalog, waves - published
 * through a single {@code volatile}, never field by field. If you add state here, say so here.
 */
public class GameWorld {

    private final GameHost mainApp;
    private final RandomSource random;
    private final EconomyLedger economy = new EconomyLedger();
    private final EnemyRoster enemies;
    private final TowerRoster towers;
    private final ProjectileRoster projectiles = new ProjectileRoster();
    private final WaveAnnouncer waves = new WaveAnnouncer();
    // The level-scoped state GameWorld owns directly rather than delegating, as ONE immutable
    // value behind ONE volatile: the board, path, cell grid, enemy catalog and wave list are
    // correlated, and publishing them independently let the game-loop thread pair a board from
    // the incoming level with a cell grid from the outgoing one. See LoadedLevel and
    // CLAUDE.md 3 rule 1.
    private volatile LoadedLevel level = LoadedLevel.none();

    public GameWorld(GameHost mainApp) {
        this(mainApp, RandomSource.shared());
    }

    /**
     * For a run that has to be reproducible - see {@code td.BalanceHarness}.
     */
    public GameWorld(GameHost mainApp, RandomSource random) {
        this.random = random;
        this.mainApp = mainApp;
        this.enemies = new EnemyRoster(mainApp);
        this.towers = new TowerRoster(mainApp, this.economy, this::getBoard);
    }

    /**
     * The player's credits, score and lives.
     */
    public EconomyLedger economy() {
        return this.economy;
    }

    /**
     * The live enemies of the wave in play. Also the {@code EnemyRegistry} readers depend on.
     */
    public EnemyRoster enemies() {
        return this.enemies;
    }

    /**
     * The towers on the board, and their buy/sell lifecycle.
     */
    public TowerRoster towers() {
        return this.towers;
    }

    /**
     * The shells and missiles currently in flight.
     */
    public ProjectileRoster projectiles() {
        return this.projectiles;
    }

    /**
     * The "a wave started" broadcast hub.
     */
    public WaveAnnouncer waves() {
        return this.waves;
    }

    /**
     * Where anything in the simulation that needs randomness gets it - never {@code Math.random()}.
     */
    public RandomSource random() {
        return this.random;
    }

    /**
     * Seeds the roster's alive count from every path's wave about to run - summed, since a
     * round's enemies all share one roster and one alive count regardless of which path spawned
     * them, which is what makes "the round is cleared" wait for every path automatically - and
     * announces the start, in that order: a listener reacting to the announcement must not see a
     * stale count. The one method here that coordinates two collaborators rather than handing
     * one out.
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
     * The level currently installed, as one consistent snapshot. <strong>A caller that needs
     * two of its parts together must read it once and use that value</strong> - calling
     * {@link #getBoard()} and then {@link #getPath()} is two reads of the volatile and can
     * straddle a level change.
     */
    public LoadedLevel level() {
        return this.level;
    }

    /**
     * Installs a level as one atomic publication. The production path -
     * {@code GameEngine.loadLevel} builds the whole {@link LoadedLevel} and hands it over here
     * in a single write, so no reader can see a half-installed level.
     */
    public void installLevel(LoadedLevel level) {
        this.level = level;
    }

    /**
     * The installed level's board of cells - {@link CellGrid#empty()} when none is.
     */
    public CellGrid cells() {
        return this.level.cells();
    }

    public BoardGeometry getBoard() {
        return this.level.board();
    }

    /**
     * Replaces one part of the installed level.
     * <p>
     * <strong>For single-threaded worlds only</strong> - a test building a world up piece by
     * piece, or {@code td.ui.PanelEnemy}'s display-only world, which repositions its preview
     * mobs by swapping a one-point path. Each is a read-modify-write of {@link #level}, which
     * is safe precisely because nothing else is touching that world. The world the simulation
     * runs in installs a level through {@link #installLevel} instead, in one write.
     */
    public void setBoard(BoardGeometry board) {
        this.level = this.level.withBoard(board);
    }

    public Path getPath() {
        return this.level.pathAt(0);
    }

    /**
     * Replaces one part of the installed level - see {@link #setBoard}.
     */
    public void setPath(Path path) {
        this.level = this.level.withSinglePath(path);
    }

    public EnemyCatalog getEnemyCatalog() {
        return this.level.catalog();
    }

    /**
     * Replaces one part of the installed level - see {@link #setBoard}.
     */
    public void setEnemyCatalog(EnemyCatalog enemyCatalog) {
        this.level = this.level.withCatalog(enemyCatalog);
    }

    public void setInfoText(String s) {
        this.mainApp.setInfoText(s);
    }

}
