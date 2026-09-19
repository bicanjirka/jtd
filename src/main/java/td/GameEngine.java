package td;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import td.board.BoardGeometry;
import td.cell.Cell;
import td.cell.CellGrid;
import td.economy.EconomyDelta;
import td.enemy.EnemyCatalog;
import td.enemy.EnemyDefinition;
import td.enemy.EnemyMob;
import td.enemy.Rank;
import td.level.LevelDefinition;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.util.GameHost;
import td.util.GameWorld;
import td.util.LoadedLevel;
import td.util.PathRuntime;
import td.util.RandomSource;
import td.util.ThreadConfined;
import td.wave.Path;
import td.wave.PathBuilder;
import td.wave.PathCoverage;
import td.wave.PathDefinition;
import td.wave.Point;
import td.wave.Wave;
import td.wave.WaveDefinition;
import td.wave.WaveProgress;
import td.wave.WaveScript;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Owns the game state and input handling that {@link TowerDefense} used to
 * hold directly, minus anything Swing-specific. Nothing here constructs a
 * window, touches a Graphics2D, or requires a display - it can be built,
 * driven, and asserted on entirely from a test.
 * <p>
 * {@link #mouseClicked}/{@link #highlightCell} take board-relative pixel
 * coordinates (0,0 = top-left of the game board), not screen coordinates -
 * translating a real MouseEvent's screen position into that is a UI concern
 * {@link TowerDefense} still owns.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
// debugSpawnCursor is advanced by a key event; everything else here is volatile or in LoadedLevel
public class GameEngine {

    private static final Logger LOG = LoggerFactory.getLogger(GameEngine.class);

    private final GameWorld gameWorld;
    private final TowerPlacement placement;

    // The level itself - cell grid, board, path, catalog and wave list - is NOT here: it is
    // one correlated bundle, so it lives behind GameWorld's single volatile LoadedLevel (see
    // CLAUDE.md 3 rule 1). What remains here is per-run progress, and each of these genuinely
    // is an independent scalar written on one thread and read on the other: the EDT loads a
    // level, requests a wave and sells towers; the game-loop thread consumes those requests in
    // doTick and advances the wave counter. Pair the wave counter with the wave list only
    // through waveProgress(), which reads one snapshot of each.
    private volatile int wave = 0;
    private volatile boolean waveReady = true;
    private volatile boolean startWave = false;
    // EDT-only: the debug keybinding that advances it is a key event.
    private int debugSpawnCursor = 0;

    public GameEngine(GameHost host) {
        this(new GameWorld(host));
    }

    /**
     * For a run that has to be reproducible - see {@code td.BalanceHarness}.
     */
    public GameEngine(GameHost host, RandomSource random) {
        this(new GameWorld(host, random));
    }

    private GameEngine(GameWorld gameWorld) {
        this.gameWorld = gameWorld;
        // gameWorld::cells rather than a lambda closing over this: nothing here hands a
        // partially-constructed GameEngine to a collaborator.
        this.placement = new TowerPlacement(this.gameWorld, this.gameWorld::cells);
    }

    public GameWorld getGameWorld() {
        return this.gameWorld;
    }

    public List<Tower> getTowers() {
        return this.gameWorld.towers().all();
    }

    /**
     * The board, as a queryable grid rather than the backing array - see {@link CellGrid}.
     * Never null: a level that has not been loaded yields {@link CellGrid#empty()}.
     */
    public CellGrid cells() {
        return this.gameWorld.cells();
    }

    public int getCurrentWaveIndex() {
        return this.wave;
    }

    public int getWaveCount() {
        return this.gameWorld.level().waveCount();
    }

    /**
     * Wave index, wave count and the waves either side of the cursor, read from <em>one</em>
     * snapshot of the installed level. Callers needing more than one of those must use this
     * rather than combining {@link #getCurrentWaveIndex()} with {@link #getWaveCount()}: those
     * are two reads, and a level installing between them pairs an index belonging to one level
     * with a count belonging to another. See {@link WaveProgress}.
     */
    public WaveProgress waveProgress() {
        LoadedLevel installed = this.gameWorld.level();
        int index = this.wave;
        int count = installed.waveCount();
        if (index > count) {
            // The wave counter still belongs to a level that is no longer installed - report
            // the incoming level rather than indexing off the end of its shorter wave list.
            return new WaveProgress(count, count, List.of(), List.of());
        }
        List<Wave> current = index > 0 ? installed.wavesAt(index - 1) : List.of();
        List<Wave> next = index < count ? installed.wavesAt(index) : List.of();
        return new WaveProgress(index, count, current, next);
    }

    /**
     * Whether the previous wave is cleared, so the next one is allowed to start.
     */
    public boolean isWaveReady() {
        return this.waveReady;
    }

    public void setWaveReady(boolean ready) {
        this.waveReady = ready;
    }

    public boolean isPlacingTower() {
        return this.placement.isPlacing();
    }

    /**
     * Turns a level into live engine state: cell grid, path, buildability, waves and starting
     * economy. This is the single entry point for doing so, and it is idempotent - always safe
     * to call from any prior state, not just once per process, since returning to the
     * level-select menu and picking another level calls it again on the same engine. See
     * {@link #unloadCurrentLevel()} for the ordering that makes that safe.
     */
    public void loadLevel(LevelDefinition level) {
        unloadCurrentLevel();
        int width = level.width();
        int height = level.height();
        int scale = this.gameWorld.getBoard().scale();
        // Built fully, then published: a publishing write must hand over a finished object,
        // never one another thread could see mid-fill.
        CellGrid grid = CellGrid.of(width, height, scale);
        EnemyCatalog catalog = EnemyCatalog.builtIn();
        level.customEnemies().forEach(catalog::register);
        level.customRankedEnemies().forEach(catalog::register);

        // Every path is built independently, against the catalog local rather than through the
        // world. A Wave holds only its content until it starts (see Wave.spawn), so nothing here
        // reads the level state this method is in the middle of replacing.
        List<PathRuntime> pathRuntimes = new ArrayList<>();
        List<PathDefinition> pathDefinitions = level.paths();
        for (int pathIndex = 0; pathIndex < pathDefinitions.size(); pathIndex++) {
            PathDefinition pathDefinition = pathDefinitions.get(pathIndex);
            Path path = PathBuilder.build(pathDefinition.corners(), pathDefinition.smoothing(), scale);
            List<Wave> pathWaves = new ArrayList<>();
            List<WaveDefinition> waveDefinitions = pathDefinition.waves();
            for (int round = 0; round < waveDefinitions.size(); round++) {
                WaveDefinition wd = waveDefinitions.get(round);
                // Derived from the level's own name and this path/round, not GameWorld.random() -
                // the same level, path and round must scatter the same way on every run and every
                // machine, and String.hashCode() is specified by the JLS to be stable across JVMs
                // for that.
                long scatterSeed = ((long) level.name().hashCode() * 31L + pathIndex) * 31L + round;
                float speedMultiplier = pathDefinition.speedMultiplier() * wd.speedMultiplier();
                pathWaves.add(new Wave(this.gameWorld, WaveScript.parse(wd.enemies(), wd.rank(), catalog),
                        scatterSeed, pathIndex, speedMultiplier));
            }
            pathRuntimes.add(new PathRuntime(path, pathWaves, pathDefinition.color()));
        }
        markUnbuildableCells(grid, pathRuntimes, scale);

        // One write. Everything above filled a local; none of it is reachable by the game-loop
        // thread until this line publishes every part at once. See LoadedLevel.
        this.wave = 0;
        this.gameWorld.installLevel(new LoadedLevel(grid,
                BoardGeometry.of(scale, width, height), pathRuntimes, catalog));

        this.gameWorld.economy().startEconomy(level.startingCredits(), level.startingLives());
        LOG.info("Level loaded: {} ({}x{} board, {} paths, {} waves per path, {} starting credits, {} starting lives)",
                level.name(), width, height, pathRuntimes.size(),
                pathRuntimes.isEmpty() ? 0 : pathRuntimes.getFirst().waves().size(),
                level.startingCredits(), level.startingLives());
    }

    /**
     * Makes {@link #loadLevel} safe to call from a dirty state - a level already in play, or
     * another level's leftovers - by discarding everything the outgoing level owned before any
     * new geometry or grid is installed. The ordering matters: {@link GameWorld#clearTowers()}
     * maps each tower's pixel position back to a cell through the CURRENT {@code BoardGeometry}
     * and calls back into the CURRENT {@code cellGrid} via {@link #clearCell}, so it must run
     * against the outgoing board, before {@code loadLevel} replaces it below.
     */
    private void unloadCurrentLevel() {
        this.placement.reset();
        this.gameWorld.projectiles().clear();
        this.gameWorld.towers().clear();
        this.gameWorld.enemies().clear();
        this.startWave = false;
        this.waveReady = true;
    }

    /**
     * Marks unbuildable every cell any path's actual geometry covers - not just the cells it
     * was authored through, so a smoothed/curved path's buildable set correctly reflects its
     * real shape. Every path affects buildability identically and the result is unioned, so no
     * tower is buildable on any path regardless of how many a level has. See {@link PathCoverage}
     * for the geometry itself.
     */
    private void markUnbuildableCells(CellGrid grid, List<PathRuntime> pathRuntimes, int scale) {
        Set<Point> unbuildable = new HashSet<>();
        for (PathRuntime pathRuntime : pathRuntimes) {
            unbuildable.addAll(PathCoverage.unbuildableCells(pathRuntime.path().points(), scale, grid.width(), grid.height()));
        }
        for (Point cell : unbuildable) {
            grid.at(cell.x(), cell.y()).enable(false);
        }
    }

    /**
     * Opens the gate on the first wave, once a level is loaded and the player is ready to play it.
     */
    public void startLevel() {
        this.waveReady = true;
    }

    /**
     * Asks for the next wave without starting it here. The request is a flag {@link #doTick}
     * consumes on the game-loop thread, so spawning always happens in tick order - the UI
     * (which calls this from the EDT) never mutates the enemy roster itself.
     */
    public void requestNextWave() {
        this.startWave = true;
    }

    /**
     * @return true if a new wave actually started (i.e. one was ready and available)
     */
    public boolean nextWave() {
        LoadedLevel installed = this.gameWorld.level();
        if (this.waveReady && this.wave < installed.waveCount()) {
            this.startWave = false;
            this.waveReady = false;
            List<Wave> starting = installed.wavesAt(this.wave);
            List<EnemyMob> spawned = new ArrayList<>();
            int totalEnemies = 0;
            for (Wave w : starting) {
                spawned.addAll(List.of(w.spawn()));
                totalEnemies += w.enemyCount();
            }
            this.gameWorld.enemies().setEnemies(spawned.toArray(new EnemyMob[0]));
            this.gameWorld.startWave(starting);
            this.wave++;
            LOG.info("Round {}/{} started, {} paths, {} enemies", this.wave, installed.waveCount(),
                    starting.size(), totalEnemies);
            return true;
        }
        return false;
    }

    /**
     * @return true if this tick started a new wave (caller may want to refresh UI accordingly)
     */
    public boolean doTick(int time) {
        LOG.debug("doTick t={}", time);
        boolean waveStarted = false;
        if (this.startWave) {
            waveStarted = this.nextWave();
        }
        for (EnemyMob enemy : this.gameWorld.enemies().getEnemies()) {
            enemy.doTick(time);
        }
        // Between enemies and towers: a projectile in flight aims at this tick's enemy
        // positions, and one a tower spawns below first advances next tick rather than
        // moving twice (once here, once after being added) in the tick it was fired.
        this.gameWorld.projectiles().doTick(time);
        for (Tower tower : this.gameWorld.towers().all()) {
            tower.doTick(time);
        }
        return waveStarted;
    }

    public void startPlacing(TowerFactory.Type t, float r) {
        this.placement.start(t, r);
    }

    public void cancelPlacing() {
        this.placement.cancel();
    }

    public void unSelectTower() {
        this.placement.unSelectTower();
    }

    /**
     * Frees the cell a sold (or torn-down) tower occupied, making it buildable again. Reached
     * from {@code TowerRoster} through {@link GameHost#clearCell}, which is how the tower
     * roster stays ignorant of the cell grid.
     */
    public void clearCell(int x, int y) {
        Cell cell = this.gameWorld.cells().at(x, y);
        cell.unSetTower();
        cell.enable(true);
    }

    public void highlightCell(int boardX, int boardY) {
        this.placement.highlightCell(boardX, boardY);
    }

    /**
     * @return the tower now selected by clicking its occupied cell, or empty if nothing was
     * selected
     */
    public Optional<Tower> mouseClicked(int boardX, int boardY) {
        return this.placement.mouseClicked(boardX, boardY);
    }

    /**
     * Debug tool: clears the current wave's enemies with no penalty (the same teardown path
     * {@link #unloadCurrentLevel()} uses - no bounty, no score, no "you won" notification, since
     * skipping is neither a kill nor a real clear) and starts the next one immediately.
     *
     * @return true if a next wave actually started (false on the last wave, where the board is
     * still cleared but there is nothing left to advance to)
     */
    public boolean debugSkipCurrentWave() {
        this.gameWorld.enemies().clear();
        this.waveReady = true;
        return this.nextWave();
    }

    /**
     * Debug tool: spawns one instance of the next id in {@link EnemyCatalog#ids()}, cycling
     * back to the first once every id has been used. Uses the definition's own
     * {@code baseHealth}/{@code price} (there is no wave context to scale from) and appears
     * immediately at the path's start.
     *
     * @return the id spawned, or empty if no level is loaded
     */
    public Optional<String> debugSpawnNextCatalogEnemy() {
        if (!this.gameWorld.level().isLoaded()) {
            return Optional.empty();
        }
        EnemyCatalog catalog = this.gameWorld.getEnemyCatalog();
        List<String> ids = catalog.ids();
        String id = ids.get(this.debugSpawnCursor % ids.size());
        this.debugSpawnCursor++;
        EnemyDefinition definition = catalog.get(id);
        EnemyMob mob = catalog.spawn(id, this.gameWorld, 0, definition.baseHealth(), definition.price(), Rank.GRUNT);
        this.gameWorld.enemies().add(mob);
        return Optional.of(id);
    }

    /**
     * Debug tool: grants a lump sum of credits, through the same path a kill or a sale uses.
     */
    public void debugGrantCredits(int amount) {
        this.gameWorld.economy().apply(EconomyDelta.credits(amount));
    }
}
