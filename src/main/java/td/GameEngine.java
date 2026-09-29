package td;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import td.board.BoardGeometry;
import td.cell.Cell;
import td.cell.CellGrid;
import td.economy.EconomyDelta;
import td.enemy.EnemyCatalog;
import td.enemy.EnemyDefinition;
import td.enemy.EnemyInspection;
import td.enemy.EnemyMob;
import td.enemy.Rank;
import td.level.LevelDefinition;
import td.level.LevelOutcome;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.tower.upgrade.UpgradeNode;
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
 * Game state and input semantics, with nothing Swing-specific: it can be built and driven entirely
 * from a test.
 * <p>
 * {@link #mouseClicked} and {@link #highlightCell} take board-relative pixels (0,0 is the board's
 * top-left), not screen coordinates.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
public class GameEngine {

    private static final Logger LOG = LoggerFactory.getLogger(GameEngine.class);

    private final GameWorld gameWorld;
    private final TowerPlacement placement;

    // Pair with the wave list only through waveProgress().
    private volatile int wave = 0;
    private volatile boolean waveReady = true;
    private volatile boolean startWave = false;
    private volatile LevelOutcome outcome = LevelOutcome.PLAYING;
    private int debugSpawnCursor = 0;

    public GameEngine(GameHost host) {
        this(new GameWorld(host));
    }

    /** For a run that has to be reproducible. */
    public GameEngine(GameHost host, RandomSource random) {
        this(new GameWorld(host, random));
    }

    private GameEngine(GameWorld gameWorld) {
        this.gameWorld = gameWorld;
        this.placement = new TowerPlacement(this.gameWorld, this.gameWorld::cells);
    }

    public GameWorld getGameWorld() {
        return this.gameWorld;
    }

    public List<Tower> getTowers() {
        return this.gameWorld.towers().all();
    }

    /** Never null: before a level loads this is {@link CellGrid#empty()}. */
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
     * Wave index, count and neighbouring waves from one snapshot of the installed level. Use this
     * rather than combining {@link #getCurrentWaveIndex()} with {@link #getWaveCount()}: a level
     * installing between those two reads mixes two levels.
     */
    public WaveProgress waveProgress() {
        LoadedLevel installed = this.gameWorld.level();
        int index = this.wave;
        int count = installed.waveCount();
        if (index > count) {
            // The wave counter can outrun a freshly installed, shorter level.
            return new WaveProgress(count, count, List.of(), List.of());
        }
        List<Wave> current = index > 0 ? installed.wavesAt(index - 1) : List.of();
        List<Wave> next = index < count ? installed.wavesAt(index) : List.of();
        return new WaveProgress(index, count, current, next);
    }

    public boolean isWaveReady() {
        return this.waveReady;
    }

    public LevelOutcome outcome() {
        return this.outcome;
    }

    public boolean isPlacingTower() {
        return this.placement.isPlacing();
    }

    /**
     * Turns a level into live engine state. Idempotent: safe from any prior state, since returning
     * to the menu and picking another level calls it again.
     */
    public void loadLevel(LevelDefinition level) {
        unloadCurrentLevel();
        this.gameWorld.enemySelection().requestClear();
        int width = level.width();
        int height = level.height();
        int scale = this.gameWorld.getBoard().scale();
        CellGrid grid = CellGrid.of(width, height, scale);
        EnemyCatalog catalog = EnemyCatalog.builtIn();
        level.customEnemies().forEach(catalog::register);
        level.customRankedEnemies().forEach(catalog::register);

        List<PathRuntime> pathRuntimes = new ArrayList<>();
        List<PathDefinition> pathDefinitions = level.paths();
        for (int pathIndex = 0; pathIndex < pathDefinitions.size(); pathIndex++) {
            PathDefinition pathDefinition = pathDefinitions.get(pathIndex);
            Path path = PathBuilder.build(pathDefinition.corners(), pathDefinition.smoothing(), scale);
            List<Wave> pathWaves = new ArrayList<>();
            List<WaveDefinition> waveDefinitions = pathDefinition.waves();
            for (int round = 0; round < waveDefinitions.size(); round++) {
                WaveDefinition wd = waveDefinitions.get(round);
                // Deterministic per level, path and round across runs and JVMs: String.hashCode is
                // specified by the JLS.
                long scatterSeed = ((long) level.name().hashCode() * 31L + pathIndex) * 31L + round;
                float speedMultiplier = pathDefinition.speedMultiplier() * wd.speedMultiplier();
                pathWaves.add(new Wave(this.gameWorld, WaveScript.parse(wd.enemies(), wd.rank(), catalog),
                        scatterSeed, pathIndex, speedMultiplier));
            }
            pathRuntimes.add(new PathRuntime(path, pathWaves, pathDefinition.color()));
        }
        markUnbuildableCells(grid, pathRuntimes, scale);

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
     * Discards everything the outgoing level owned. Must run before the new board is installed:
     * clearing towers maps their positions back to cells through the current board.
     */
    private void unloadCurrentLevel() {
        this.placement.reset();
        this.gameWorld.projectiles().clear();
        this.gameWorld.towers().clear();
        this.gameWorld.enemies().clear();
        this.gameWorld.damageTally().clear();
        this.startWave = false;
        this.waveReady = true;
        this.outcome = LevelOutcome.PLAYING;
    }

    /**
     * Marks unbuildable every cell any path's real geometry covers, not just its authored corners.
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

    public void startLevel() {
        this.waveReady = true;
    }

    /**
     * Only sets a flag that {@link #doTick} consumes, so spawning always happens on the game-loop
     * thread in tick order.
     */
    public void requestNextWave() {
        this.startWave = true;
    }

    /**
     * @return whether a wave actually started
     */
    public boolean nextWave() {
        LoadedLevel installed = this.gameWorld.level();
        if (!this.outcome.isOver() && this.waveReady && this.wave < installed.waveCount()) {
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
     * A no-op once the level is over, so nothing can change the outcome.
     *
     * @return whether this tick started a new wave
     */
    public boolean doTick(int time) {
        if (this.outcome.isOver()) {
            return false;
        }
        LOG.debug("doTick t={}", time);
        boolean waveStarted = false;
        if (this.startWave) {
            waveStarted = this.nextWave();
        }
        this.gameWorld.disruptions().clear();
        for (EnemyMob enemy : this.gameWorld.enemies().getEnemies()) {
            enemy.doTick(time);
        }
        this.gameWorld.projectiles().doTick(time);
        for (Tower tower : this.gameWorld.towers().all()) {
            tower.refreshDisruption();
            tower.doTick(time);
        }
        this.settleDeaths(time);
        this.settleWave();
        return waveStarted;
    }

    /**
     * Lets every mob killed this tick run its on-death abilities now. A mob only notices its death
     * on its own next tick, which would land after {@link #settleWave} had already counted the
     * board as empty.
     */
    private void settleDeaths(int time) {
        for (EnemyMob enemy : this.gameWorld.enemies().getEnemies()) {
            if (enemy.isDead()) {
                enemy.doTick(time);
            }
        }
    }

    /**
     * Checked after the whole tick rather than on each death, so a death that spawns replacements
     * later in the same tick does not read as a cleared wave. Loss is checked first: a last enemy
     * leaking the last life loses.
     */
    private void settleWave() {
        LoadedLevel installed = this.gameWorld.level();
        if (this.gameWorld.economy().state().isGameOver()) {
            this.endLevel(LevelOutcome.LOST);
        } else if (this.wave > 0 && !this.waveReady && this.gameWorld.enemies().aliveCount() == 0) {
            if (this.wave < installed.waveCount()) {
                LOG.info("Wave {} cleared, ready for the next one", this.wave);
                this.waveReady = true;
            } else {
                this.endLevel(LevelOutcome.WON);
            }
        }
    }

    private void endLevel(LevelOutcome reached) {
        this.outcome = reached;
        LOG.info("Level over - {}, score={}", reached, this.gameWorld.economy().getScore());
    }

    public void startPlacing(TowerFactory.Type t, float r) {
        if (!this.outcome.isOver()) {
            this.placement.start(t, r);
        }
    }

    public void cancelPlacing() {
        this.placement.cancel();
    }

    public void unSelectTower() {
        this.placement.unSelectTower();
    }

    /**
     * Frees a sold tower's cell. Reached through {@link GameHost#clearCell}, which keeps the tower
     * roster ignorant of the grid.
     */
    public void clearCell(int x, int y) {
        Cell cell = this.gameWorld.cells().at(x, y);
        cell.unSetTower();
        cell.enable(true);
    }

    public void highlightCell(int boardX, int boardY) {
        this.placement.highlightCell(boardX, boardY);
    }

    /** Any thread: selects the enemy nearest this board pixel on the next frame build. */
    public void requestEnemySelectionAt(int boardX, int boardY) {
        this.gameWorld.enemySelection().requestAt(boardX, boardY);
    }

    /** Any thread: drops the enemy selection on the next frame build. */
    public void clearEnemySelection() {
        this.gameWorld.enemySelection().requestClear();
    }

    /**
     * Game-loop thread, once per frame build: applies pending selection requests and snapshots the
     * selected enemy.
     */
    public Optional<EnemyInspection> inspectSelectedEnemy() {
        return this.gameWorld.enemySelection().resolve(this.gameWorld.enemies());
    }

    /**
     * After the level is over a click can still select a tower to inspect, but never builds one.
     *
     * @return the tower selected by the click, or empty
     */
    public Optional<Tower> mouseClicked(int boardX, int boardY) {
        if (this.outcome.isOver()) {
            this.placement.cancel();
        }
        return this.placement.mouseClicked(boardX, boardY);
    }

    /**
     * Debug: clears the current wave with no bounty or score and starts the next; skipping the
     * last wave wins.
     *
     * @return whether a next wave started; false on the last wave
     */
    public boolean debugSkipCurrentWave() {
        if (this.outcome.isOver() || !this.gameWorld.level().isLoaded()) {
            return false;
        }
        this.gameWorld.enemies().clear();
        this.waveReady = true;
        if (this.nextWave()) {
            return true;
        }
        this.endLevel(LevelOutcome.WON);
        return false;
    }

    /**
     * Debug: spawns the next catalog id at the path start with its base health and price, cycling
     * through the catalog.
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

    /** Debug: grants credits through the normal economy path. */
    public void debugGrantCredits(int amount) {
        this.gameWorld.economy().apply(EconomyDelta.credits(amount));
    }

    /**
     * Buys the selected tower's {@code number}-th (1-based) offered upgrade, in the order the
     * upgrade panel numbers its buttons. A no-op if nothing is selected or the number is out of
     * range.
     */
    public void buyUpgradeForSelected(int number) {
        if (this.outcome.isOver()) {
            return;
        }
        this.gameWorld.towers().all().stream()
                .filter(Tower::isSelected)
                .findFirst()
                .ifPresent(tower -> {
                    List<UpgradeNode> offered = tower.offeredUpgrades(this.gameWorld);
                    if (number >= 1 && number <= offered.size()) {
                        tower.buyUpgrade(offered.get(number - 1));
                    }
                });
    }
}
