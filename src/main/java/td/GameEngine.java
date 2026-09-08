package td;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import td.board.BoardGeometry;
import td.cell.Cell;
import td.cell.CellNormal;
import td.enemy.EnemyMob;
import td.level.LevelDefinition;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.util.GameHost;
import td.util.GameWorld;
import td.wave.Path;
import td.wave.PathBuilder;
import td.wave.PathCoverage;
import td.wave.Point;
import td.wave.Wave;
import td.wave.WaveDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * Owns the game state and input handling that {@link TowerDefense} used to
 * hold directly, minus anything Swing-specific. Nothing here constructs a
 * window, touches a Graphics2D, or requires a display - it can be built,
 * driven, and asserted on entirely from a test.
 * <p>
 * {@code mouseClicked}/{@code highlightCell} take board-relative pixel
 * coordinates (0,0 = top-left of the game board), not screen coordinates -
 * translating a real MouseEvent's screen position into that is a UI concern
 * TowerDefence still owns.
 */
public class GameEngine {

    private static final Logger LOG = LoggerFactory.getLogger(GameEngine.class);

    private final GameWorld context;
    private final TowerPlacement placement;

    private Cell[][] cellGrid;
    private List<Wave> waves = new ArrayList<>();
    private int wave = 0;
    private boolean waveReady = true;
    private boolean startWave = false;

    public GameEngine(GameHost host) {
        this.context = new GameWorld(host);
        this.placement = new TowerPlacement(this.context, () -> this.cellGrid);
    }

    public GameWorld getGameWorld() {
        return this.context;
    }

    public List<Tower> getTowers() {
        return this.context.getTowers();
    }

    public Cell[][] getCellGrid() {
        return this.cellGrid;
    }

    public int getCurrentWaveIndex() {
        return this.wave;
    }

    public int getWaveCount() {
        return this.waves.size();
    }

    public Wave getWaveAt(int index) {
        return this.waves.get(index);
    }

    public boolean isWaveReady() {
        return this.waveReady;
    }

    public void setWaveReady(boolean ready) {
        this.waveReady = ready;
    }

    public boolean isPlacingTower() {
        return this.placement.isPlacing();
    }

    public void loadLevel(LevelDefinition level) {
        int width = level.width();
        int height = level.height();
        int scale = this.context.getBoard().scale();
        this.cellGrid = new Cell[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                this.cellGrid[i][j] = new CellNormal(i * scale, j * scale);
            }
        }
        this.context.setBoard(BoardGeometry.of(scale, width, height));

        this.waves = new ArrayList<>();
        Path path = PathBuilder.build(level.path(), level.smoothing(), scale);
        this.context.setPath(path);
        markUnbuildableCells(path, scale);
        this.wave = 0;

        for (WaveDefinition wd : level.waves()) {
            this.waves.add(new Wave(this.context, wd.hp(), wd.price(), wd.level(), wd.enemies().split(" ")));
        }

        this.context.startEconomy(level.startingCredits(), level.startingLives());
        LOG.info("Level loaded: {} ({}x{} board, {} waves, {} starting credits, {} starting lives)",
                level.name(), width, height, this.waves.size(), level.startingCredits(), level.startingLives());
    }

    /**
     * Marks unbuildable every cell the path's actual geometry covers - not just the cells it
     * was authored through, so a smoothed/curved path's buildable set correctly reflects its
     * real shape. See {@link PathCoverage} for the geometry itself.
     */
    private void markUnbuildableCells(Path path, int scale) {
        int width = this.cellGrid.length;
        int height = width == 0 ? 0 : this.cellGrid[0].length;
        for (Point cell : PathCoverage.unbuildableCells(path.points(), scale, width, height)) {
            this.cellGrid[cell.x()][cell.y()].enable(false);
        }
    }

    public void startLevel() {
        this.waveReady = true;
    }

    public void requestNextWave() {
        this.startWave = true;
    }

    /**
     * @return true if a new wave actually started (i.e. one was ready and available)
     */
    public boolean nextWave() {
        if (this.waveReady && this.wave < this.waves.size()) {
            this.startWave = false;
            this.waveReady = false;
            Wave tempWave = this.waves.get(this.wave);
            this.context.setEnemies(tempWave.getEnemies());
            this.context.startWave(tempWave);
            this.wave++;
            LOG.info("Wave {}/{} started, {} enemies", this.wave, this.waves.size(), tempWave.enemyCount());
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
        for (EnemyMob enemy : this.context.getEnemies()) {
            enemy.doTick(time);
        }
        for (Tower tower : this.context.getTowers()) {
            tower.doTick(time);
        }
        return waveStarted;
    }

    public void startPlacing(TowerFactory.type t, float r) {
        this.placement.start(t, r);
    }

    public void cancelPlacing() {
        this.placement.cancel();
    }

    public void unSelectTower() {
        this.placement.unSelectTower();
    }

    public void clearCell(int x, int y) {
        Cell cell = this.cellGrid[x][y];
        cell.unSetTower();
        cell.enable(true);
    }

    public void highlightCell(int boardX, int boardY) {
        this.placement.highlightCell(boardX, boardY);
    }

    /**
     * @return the tower now selected by clicking its occupied cell, or null if nothing was selected
     */
    public Tower mouseClicked(int boardX, int boardY) {
        return this.placement.mouseClicked(boardX, boardY);
    }
}
