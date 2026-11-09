package td.util;

import td.board.BoardGeometry;
import td.cell.CellGrid;
import td.damage.DamageTally;
import td.economy.EconomyLedger;
import td.enemy.EnemyCatalog;
import td.enemy.EnemyObservation;
import td.enemy.EnemyRoster;
import td.enemy.EnemySelection;
import td.enemy.ObserveLog;
import td.enemy.WalkSettlement;
import td.projectile.ProjectileRoster;
import td.stat.DisruptionField;
import td.tower.ExperienceAwarder;
import td.tower.TowerRoster;
import td.wave.Path;
import td.wave.Wave;
import td.wave.WaveAnnouncer;
import td.zone.ZoneRoster;

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

    private final RandomSource injected;
    // Swapped as each wave starts, on the game loop; read through the one handle below, which holders keep.
    private volatile RandomSource wave;
    private final RandomSource random = () -> this.wave.nextDouble();
    private final EconomyLedger economy = new EconomyLedger();
    private final EnemyRoster enemies;
    private final TowerRoster towers;
    private final ProjectileRoster projectiles = new ProjectileRoster();
    private final ZoneRoster zones;
    private final PlaytestRules playtestRules = new PlaytestRules();
    private final PlaytestHooks playtestHooks = new PlaytestHooks();
    private final WaveAnnouncer waves = new WaveAnnouncer();
    private final DamageTally damageTally = new DamageTally();
    private final DisruptionField disruptions = new DisruptionField();
    private final EnemySelection enemySelection = new EnemySelection();
    private final EnemyObservation enemyObservation;
    private volatile LoadedLevel level = LoadedLevel.none();

    public GameWorld(GameHost mainApp) {
        this(mainApp, RandomSource.shared());
    }

    /** For a run that has to be reproducible. */
    public GameWorld(GameHost mainApp, RandomSource random) {
        this.injected = random;
        this.wave = random;
        this.enemies = new EnemyRoster();
        this.zones = new ZoneRoster(this.enemies);
        this.towers = new TowerRoster(mainApp, this.economy, this::getBoard);
        this.enemyObservation = new EnemyObservation(ObserveLog.toSessionLog(), this.towers::attackerName);
        this.enemies.addWalkEndListener(new WalkSettlement(this.economy, this.playtestRules));
        this.enemies.addWalkEndListener(new ExperienceAwarder(this.towers));
    }

    public PlaytestRules playtestRules() {
        return this.playtestRules;
    }

    /** The dev panel's own work on the board, run every tick. */
    public PlaytestHooks playtestHooks() {
        return this.playtestHooks;
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

    /** The patches of ground that keep working after the shot that made them. */
    public ZoneRoster zones() {
        return this.zones;
    }

    public WaveAnnouncer waves() {
        return this.waves;
    }

    /** Every hit that landed this level, by damage type. */
    public DamageTally damageTally() {
        return this.damageTally;
    }

    /** The enemy the player is inspecting. */
    public EnemySelection enemySelection() {
        return this.enemySelection;
    }

    /** The enemies the dev panel observes, and what names the causes in their traces. */
    public EnemyObservation enemyObservation() {
        return this.enemyObservation;
    }

    /** Where enemies weaken towers this tick. */
    public DisruptionField disruptions() {
        return this.disruptions;
    }

    /** The only source of randomness in the simulation; the same handle whatever wave draws from it. */
    public RandomSource random() {
        return this.random;
    }

    /** A seed for a new game, from the injected source. */
    public long newGameSeed() {
        return this.injected.newGameSeed();
    }

    /** From now on {@link #random()} draws from a stream started at {@code seed}. */
    public void reseed(long seed) {
        this.wave = this.injected.reseeded(seed);
    }

    /**
     * Seeds the roster's alive count with the total across every path's starting wave, then
     * announces the start - in that order, so listeners never see a stale count.
     */
    public void startWave(List<Wave> starting, int gameTime) {
        int total = 0;
        for (Wave w : starting) {
            total += w.enemyCount();
        }
        this.enemies.setCount(total);
        this.waves.announce(gameTime);
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

}
