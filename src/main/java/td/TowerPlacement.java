package td;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import td.board.BoardGeometry;
import td.cell.Cell;
import td.cell.CellGrid;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.util.GameWorld;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Tower-placement mode and the shared click-highlight state a board click needs whether it's
 * placing a new tower or selecting an already-placed one. {@code cellGrid} is a supplier
 * rather than a fixed grid since {@link GameEngine} replaces it wholesale on every
 * {@code loadLevel}, after this is constructed.
 */
public class TowerPlacement {

    private static final Logger LOG = LoggerFactory.getLogger(TowerPlacement.class);

    private final GameWorld gameWorld;
    private final Supplier<CellGrid> cellGrid;

    private boolean placingTower = false;
    private TowerFactory.Type placingTowerType;
    private float placingTowerRange = 0;
    private int[] highlitedCell;

    public TowerPlacement(GameWorld gameWorld, Supplier<CellGrid> cellGrid) {
        this.gameWorld = gameWorld;
        this.cellGrid = cellGrid;
    }

    public boolean isPlacing() {
        return this.placingTower;
    }

    public void start(TowerFactory.Type t, float r) {
        this.placingTower = true;
        this.placingTowerType = t;
        this.placingTowerRange = r;
    }

    public void cancel() {
        this.placingTower = false;
        this.unHighlightCell();
    }

    public void unSelectTower() {
        this.unHighlightCell();
    }

    /**
     * Drops placement mode and any highlighted cell without touching the grid - unlike
     * {@link #cancel()}, which un-highlights a cell in the still-current grid, this is for
     * tearing down before a new (possibly smaller) grid replaces the one {@code highlitedCell}
     * was indexing, where dereferencing it would be unsafe.
     */
    public void reset() {
        this.placingTower = false;
        this.placingTowerType = null;
        this.placingTowerRange = 0;
        this.highlitedCell = null;
    }

    private void unHighlightCell() {
        if (this.highlitedCell != null) {
            this.cellGrid.get().at(this.highlitedCell[0], this.highlitedCell[1]).setHighlight(Cell.HighlightType.none);
            this.highlitedCell = null;
        }
    }

    public void highlightCell(int boardX, int boardY) {
        this.unHighlightCell();
        BoardGeometry board = this.gameWorld.getBoard();
        if (board.containsPixel(boardX, boardY)) {
            int[] tempInt = new int[2];
            tempInt[0] = board.cellX(boardX);
            tempInt[1] = board.cellY(boardY);
            this.highlitedCell = tempInt;
            Cell cell = this.cellGrid.get().at(board.cellX(boardX), board.cellY(boardY));
            cell.setHighlight(Cell.HighlightType.place);
            cell.setHighlightRange(this.placingTowerRange);
        }
    }

    /**
     * @return the tower now selected by clicking its occupied cell, or empty if the click
     * selected nothing - a placement, a rejected placement, or a click on bare board
     */
    public Optional<Tower> mouseClicked(int boardX, int boardY) {
        Tower selected = null;
        BoardGeometry board = this.gameWorld.getBoard();
        if (board.containsPixel(boardX, boardY)) {
            Cell cell = this.cellGrid.get().at(board.cellX(boardX), board.cellY(boardY));
            if (cell.hasTower()) {
                selected = cell.getTower();
                int[] tempInt = new int[2];
                tempInt[0] = board.cellX(boardX);
                tempInt[1] = board.cellY(boardY);
                this.highlitedCell = tempInt;
                cell.setHighlight(Cell.HighlightType.select);
            } else if (this.placingTower) {
                if (cell.buildable()) {
                    int cellX = board.cellX(boardX);
                    int cellY = board.cellY(boardY);
                    if (this.gameWorld.economy().doPay(this.placingTowerType.price)) {
                        Tower tempTower = TowerFactory.createTower(this.placingTowerType, this.gameWorld, cellX, cellY);
                        this.gameWorld.towers().add(tempTower);
                        cell.setTower(tempTower);
                        cell.enable(false);
                        LOG.info("Tower placed: {} at ({},{}), credits left={}", this.placingTowerType, cellX, cellY, this.gameWorld.economy().getCredits());
                    } else {
                        LOG.info("Tower placement rejected: not enough credits for {} (need {}, have {})",
                                this.placingTowerType, this.placingTowerType.price, this.gameWorld.economy().getCredits());
                    }
                    this.placingTower = false;
                } else {
                    LOG.info("Tower placement rejected: cell ({},{}) is not buildable", board.cellX(boardX), board.cellY(boardY));
                }
            }
        }
        if (this.placingTower) {
            this.placingTower = false;
        }
        return Optional.ofNullable(selected);
    }
}
