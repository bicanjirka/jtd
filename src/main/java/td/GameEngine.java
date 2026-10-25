package td;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import td.board.BoardGeometry;
import td.cell.Cell;
import td.cell.CellGrid;
import td.economy.EconomyDelta;
import td.enemy.EnemyCatalog;
import td.enemy.EnemyInspection;
import td.enemy.EnemyMob;
import td.level.LevelDefinition;
import td.level.LevelOutcome;
import td.save.GameSave;
import td.save.TowerSave;
import td.tower.PulseTower;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.tower.upgrade.UpgradeNode;
import td.util.GameHost;
import td.util.GameWorld;
import td.util.LoadedLevel;
import td.util.PathRuntime;
import td.util.PlaytestRules;
import td.util.RandomSource;
import td.util.ThreadConfined;
import td.wave.Path;
import td.wave.PathBuilder;
import td.wave.PathCoverage;
import td.wave.PathDefinition;
import td.wave.Point;
import td.wave.RoundSummary;
import td.wave.Wave;
import td.wave.WaveDefinition;
import td.wave.WaveProgress;
import td.wave.WaveScript;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    /** Enough for a saved game's towers and upgrades, whatever they cost; the saved credits replace it after. */
    private static final int REBUILD_CREDITS = 1_000_000_000;

    private final GameWorld gameWorld;
    private final TowerPlacement placement;

    // Pair with the wave list only through waveProgress().
    private volatile String levelName = "";
    private volatile int wave = 0;
    private volatile boolean waveReady = true;
    private volatile boolean startWave = false;
    private volatile LevelOutcome outcome = LevelOutcome.PLAYING;
    // 0-based rounds an enemy escaped; swapped whole.
    private volatile Set<Integer> leakedRounds = Set.of();
    private volatile long leaksAtRoundStart = 0L;

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
            return new WaveProgress(count, this.roundsOf(installed), List.of(), List.of());
        }
        List<Wave> current = index > 0 ? installed.wavesAt(index - 1) : List.of();
        List<Wave> next = index < count ? installed.wavesAt(index) : List.of();
        return new WaveProgress(index, this.roundsOf(installed), current, next);
    }

    private List<RoundSummary> roundsOf(LoadedLevel installed) {
        Set<Integer> leaked = this.leakedRounds;
        return IntStream.range(0, installed.waveCount())
                .mapToObj(round -> new RoundSummary(installed.highestRankAt(round), installed.uniqueAt(round),
                        leaked.contains(round)))
                .toList();
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
        this.levelName = level.name();
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
        this.gameWorld.enemyObservation().endAll("level-ended");
        this.placement.reset();
        this.gameWorld.projectiles().clear();
        this.gameWorld.zones().clear();
        this.gameWorld.towers().clear();
        this.gameWorld.enemies().clear();
        this.gameWorld.damageTally().clear();
        this.startWave = false;
        this.waveReady = true;
        this.outcome = LevelOutcome.PLAYING;
        this.leakedRounds = Set.of();
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
            this.leaksAtRoundStart = this.gameWorld.enemies().leaks();
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
        this.gameWorld.enemyObservation().beginTick(time);
        boolean waveStarted = false;
        if (this.startWave) {
            waveStarted = this.nextWave();
        }
        this.gameWorld.disruptions().clear();
        for (EnemyMob enemy : this.gameWorld.enemies().getEnemies()) {
            enemy.doTick(time);
        }
        this.gameWorld.projectiles().doTick(time);
        this.gameWorld.zones().doTick(time);
        this.gameWorld.playtestHooks().doTick(time);
        List<Tower> towers = this.gameWorld.towers().all();
        boolean emissionChanged = false;
        for (Tower tower : towers) {
            emissionChanged |= tower.beginTick(time) && tower.emitsBuffs();
        }
        if (emissionChanged) {
            towers.forEach(Tower::recalculateStats);
        }
        for (Tower tower : towers) {
            tower.doTick(time);
        }
        this.settleDeaths(time);
        this.gameWorld.enemyObservation().sweep(this.gameWorld.enemies());
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
            this.noteLeaks();
            this.endLevel(LevelOutcome.LOST);
        } else if (this.wave > 0 && !this.waveReady && this.gameWorld.enemies().aliveCount() == 0) {
            this.noteLeaks();
            this.gameWorld.zones().endWave();
            if (this.wave < installed.waveCount()) {
                LOG.info("Wave {} cleared, ready for the next one", this.wave);
                this.waveReady = true;
            } else {
                this.endLevel(LevelOutcome.WON);
            }
        }
    }

    /** Marks the round in play as one an enemy escaped, if one has since it started. */
    private void noteLeaks() {
        if (this.wave > 0 && this.gameWorld.enemies().leaks() > this.leaksAtRoundStart) {
            Set<Integer> leaked = new HashSet<>(this.leakedRounds);
            leaked.add(this.wave - 1);
            this.leakedRounds = Set.copyOf(leaked);
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
        this.placement.stopPinningLeyLine();
    }

    /** The next board click picks the host of the selected Obelisk's Ley Line; a no-op for any other selection. */
    public void startPinningLeyLine() {
        if (!this.outcome.isOver() && this.selectedTower().filter(PulseTower::castsLeyLine).isPresent()) {
            this.placement.startPinningLeyLine();
        }
    }

    public void cancelPinningLeyLine() {
        this.placement.stopPinningLeyLine();
    }

    public boolean isPinningLeyLine() {
        return this.placement.isPinningLeyLine();
    }

    /**
     * Ends pinning, and pins the selected Obelisk's Ley Line to the Obelisk under this board pixel (the one it is
     * pinned to unpins it).
     *
     * @return whether a pin was asked for: false for an empty cell, a tower that is no Obelisk, or one out of reach
     */
    public boolean pinLeyLineAt(int boardX, int boardY) {
        this.placement.stopPinningLeyLine();
        Optional<Tower> caster = this.selectedTower();
        Optional<Tower> host = this.placement.towerAt(boardX, boardY);
        return caster.isPresent() && host.isPresent() && PulseTower.pinLeyLine(caster.get(), host.get());
    }

    private Optional<Tower> selectedTower() {
        return this.gameWorld.towers().all().stream().filter(Tower::isSelected).findFirst();
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

    /** The game as it stands between rounds, to load later: empty while a round runs or once the level is over. */
    public Optional<GameSave> save() {
        if (!this.waveReady || this.outcome.isOver() || !this.gameWorld.level().isLoaded()) {
            return Optional.empty();
        }
        BoardGeometry board = this.gameWorld.getBoard();
        List<TowerSave> towers = this.gameWorld.towers().all().stream()
                .map(tower -> new TowerSave(tower.getType(),
                        new Point(board.cellX(tower.getX()), board.cellY(tower.getY())),
                        tower.upgrades().owned().stream().map(UpgradeNode::id).toList(), tower.tally()))
                .toList();
        return Optional.of(new GameSave(this.levelName, this.wave + 1, this.gameWorld.economy().state(), towers));
    }

    /**
     * Puts {@code save}'s game on the board in place of the one running: its towers, its economy, and its wave next.
     * The towers are rebuilt in the order they were built, so each records the price it was bought for, and own
     * their upgrades whatever the gates. What can't be rebuilt any more, since the level or a tower changed after the
     * save, is left out and named: a game that loads only in part must not pass for the saved one.
     *
     * @return why it could not load, or what of it is missing; empty once all of it loaded
     */
    public Optional<String> restore(GameSave save) {
        LoadedLevel installed = this.gameWorld.level();
        if (!installed.isLoaded() || !save.level().equals(this.levelName)) {
            return Optional.of("The save is of " + save.level() + ": load that level first");
        }
        if (save.nextWave() > installed.waveCount()) {
            return Optional.of("The level has no wave " + save.nextWave());
        }
        this.unloadCurrentLevel();
        this.wave = save.nextWave() - 1;
        PlaytestRules rules = this.gameWorld.playtestRules();
        boolean gatesIgnored = rules.upgradeGatesIgnored();
        rules.setUpgradeGatesIgnored(true);
        this.gameWorld.economy().startEconomy(REBUILD_CREDITS, save.economy().lives());
        List<String> missing = save.towers().stream().flatMap(saved -> this.rebuild(saved).stream()).toList();
        rules.setUpgradeGatesIgnored(gatesIgnored);
        this.gameWorld.economy().startEconomy(save.economy().credits(), save.economy().lives());
        this.gameWorld.economy().apply(EconomyDelta.score(save.economy().score()));
        LOG.info("Game loaded: {} before wave {}, {} towers", save.level(), save.nextWave(), save.towers().size());
        if (missing.isEmpty()) {
            return Optional.empty();
        }
        String problem = "Loaded before wave " + save.nextWave() + ", but " + missing.size() + " not rebuilt: "
                + String.join("; ", missing);
        LOG.warn(problem);
        return Optional.of(problem);
    }

    /** Builds {@code saved} again with its upgrades; returns what of it could not be. */
    private List<String> rebuild(TowerSave saved) {
        int scale = this.gameWorld.getBoard().scale();
        String where = saved.type().displayName() + " at " + saved.cell().x() + "," + saved.cell().y();
        this.placement.start(saved.type(), 0f);
        this.placement.mouseClicked(saved.cell().x() * scale + scale / 2, saved.cell().y() * scale + scale / 2);
        Cell cell = this.gameWorld.cells().at(saved.cell().x(), saved.cell().y());
        if (!cell.hasTower()) {
            return List.of(where + " (the cell takes no tower)");
        }
        Tower tower = cell.getTower();
        List<String> missing = new ArrayList<>();
        for (String id : saved.nodes()) {
            Optional<UpgradeNode> node = tower.upgradeTree().nodes().stream()
                    .filter(candidate -> candidate.id().equals(id)).findFirst();
            if (node.isEmpty()) {
                missing.add(where + ": upgrade " + id + " (no such upgrade)");
            } else if (!tower.buyUpgrade(node.get())) {
                missing.add(where + ": upgrade " + id + " (not on offer)");
            }
        }
        tower.restore(saved.tally());
        return missing;
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
        this.noteLeaks();
        this.gameWorld.enemies().clear();
        this.gameWorld.zones().endWave();
        this.waveReady = true;
        if (this.nextWave()) {
            return true;
        }
        this.endLevel(LevelOutcome.WON);
        return false;
    }

    /**
     * Debug: clears the board with no bounty or score and makes wave {@code number} (1-based,
     * clamped to the level) the next to start.
     *
     * @return the wave that is now next, or 0 if nothing is loaded or the level is over
     */
    public int debugJumpToWave(int number) {
        if (this.outcome.isOver() || !this.gameWorld.level().isLoaded()) {
            return 0;
        }
        int next = Math.max(1, Math.min(number, this.gameWorld.level().waveCount()));
        this.gameWorld.enemies().clear();
        this.gameWorld.zones().endWave();
        this.startWave = false;
        this.wave = next - 1;
        this.waveReady = true;
        this.leakedRounds = this.leakedRounds.stream().filter(round -> round < next - 1)
                .collect(Collectors.toUnmodifiableSet());
        return next;
    }

    /** Debug: grants credits through the normal economy path. */
    public void debugGrantCredits(int amount) {
        this.gameWorld.economy().apply(EconomyDelta.credits(amount));
    }

    /**
     * Buys the selected tower's offered upgrade whose key is {@code number}
     * ({@link td.tower.upgrade.UpgradeTree#keyOf(td.tower.upgrade.UpgradeNode)}). A no-op if nothing is
     * selected or nothing on offer has that key.
     */
    public void buyUpgradeForSelected(int number) {
        if (this.outcome.isOver()) {
            return;
        }
        this.gameWorld.towers().all().stream()
                .filter(Tower::isSelected)
                .findFirst()
                .ifPresent(tower -> tower.offeredUpgrades(this.gameWorld).stream()
                        .filter(node -> tower.upgradeTree().keyOf(node) == number)
                        .findFirst()
                        .ifPresent(tower::buyUpgrade));
    }
}
