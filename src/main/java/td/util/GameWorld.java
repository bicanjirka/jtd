package td.util;

import td.board.BoardGeometry;
import td.economy.EconomyLedger;
import td.enemy.EnemyCatalog;
import td.enemy.EnemyRoster;
import td.projectile.ProjectileRoster;
import td.tower.TowerRoster;
import td.wave.Path;
import td.wave.PathNormal;
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
 * It is <em>not</em> a pure composition root. Four things are its own state rather than a
 * collaborator's: the {@link BoardGeometry}, the {@link Path}, the {@link EnemyCatalog} and
 * the {@link RandomSource}. The first three are level-scoped values replaced wholesale on
 * load; see the field comments for why they are {@code volatile}. If you add state here, say
 * so here.
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

    /** The player's credits, score and lives. */
    public EconomyLedger economy() {
        return this.economy;
    }

    /** The live enemies of the wave in play. Also the {@code EnemyRegistry} readers depend on. */
    public EnemyRoster enemies() {
        return this.enemies;
    }

    /** The towers on the board, and their buy/sell lifecycle. */
    public TowerRoster towers() {
        return this.towers;
    }

    /** The shells and missiles currently in flight. */
    public ProjectileRoster projectiles() {
        return this.projectiles;
    }

    /** The "a wave started" broadcast hub. */
    public WaveAnnouncer waves() {
        return this.waves;
    }

    /** Where anything in the simulation that needs randomness gets it - never {@code Math.random()}. */
    public RandomSource random() {
        return this.random;
    }

    /**
     * Seeds the roster's alive count from the wave about to run and announces the start, in
     * that order - a listener reacting to the announcement must not see a stale count. The one
     * method here that coordinates two collaborators rather than handing one out.
     */
    public void startWave(Wave w) {
        this.enemies.setCount(w.enemyCount());
        this.waves.announce();
    }

    public BoardGeometry getBoard() {
        return this.board;
    }

    public void setBoard(BoardGeometry board) {
        this.board = board;
    }

    public Path getPath() {
        return this.path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public EnemyCatalog getEnemyCatalog() {
        return this.enemyCatalog;
    }

    public void setEnemyCatalog(EnemyCatalog enemyCatalog) {
        this.enemyCatalog = enemyCatalog;
    }

    public void setInfoText(String s) {
        this.mainApp.setInfoText(s);
    }

}
