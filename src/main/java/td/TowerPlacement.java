package td;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import td.board.BoardGeometry;
import td.cell.Cell;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.util.GameWorld;

import java.util.function.Supplier;

/**
 * Tower-placement mode and the shared click-highlight state a board click needs whether it's
 * placing a new tower or selecting an already-placed one. {@code cellGrid} is a supplier
 * rather than a fixed array since {@link GameEngine} replaces it wholesale on every
 * {@code loadLevel}, after this is constructed.
 */
public class TowerPlacement {

    private static final Logger LOG = LoggerFactory.getLogger(TowerPlacement.class);

    private final GameWorld context;
    private final Supplier<Cell[][]> cellGrid;

    private boolean placingTower = false;
    private TowerFactory.type placingTowerType;
    private float placingTowerRange = 0;
    private int[] highlitedCell;

    public TowerPlacement(GameWorld context, Supplier<Cell[][]> cellGrid) {
        this.context = context;
        this.cellGrid = cellGrid;
    }

    public boolean isPlacing() {
        return this.placingTower;
    }

    public void start(TowerFactory.type t, float r) {
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

    private void unHighlightCell() {
        if (this.highlitedCell != null) {
            this.cellGrid.get()[this.highlitedCell[0]][this.highlitedCell[1]].setHighlight(Cell.highlightType.none);
            this.highlitedCell = null;
        }
    }

    public void highlightCell(int boardX, int boardY) {
        this.unHighlightCell();
        BoardGeometry board = this.context.getBoard();
        if (board.containsPixel(boardX, boardY)) {
            int[] tempInt = new int[2];
            tempInt[0] = board.cellX(boardX);
            tempInt[1] = board.cellY(boardY);
            this.highlitedCell = tempInt;
            Cell cell = this.cellGrid.get()[board.cellX(boardX)][board.cellY(boardY)];
            cell.setHighlight(Cell.highlightType.place);
            cell.setHighlightRange(this.placingTowerRange);
        }
    }

    /**
     * @return the tower now selected by clicking its occupied cell, or null if nothing was selected
     */
    public Tower mouseClicked(int boardX, int boardY) {
        Tower selected = null;
        BoardGeometry board = this.context.getBoard();
        if (board.containsPixel(boardX, boardY)) {
            Cell cell = this.cellGrid.get()[board.cellX(boardX)][board.cellY(boardY)];
            if (cell.hasTower()) {
                selected = cell.getTower();
                int[] tempInt = new int[2];
                tempInt[0] = board.cellX(boardX);
                tempInt[1] = board.cellY(boardY);
                this.highlitedCell = tempInt;
                cell.setHighlight(Cell.highlightType.select);
            } else if (this.placingTower) {
                if (cell.buildable()) {
                    int cellX = board.cellX(boardX);
                    int cellY = board.cellY(boardY);
                    if (this.context.doPay(this.placingTowerType.price)) {
                        Tower tempTower = TowerFactory.createTower(this.placingTowerType, this.context, cellX, cellY);
                        this.context.addTower(tempTower);
                        cell.setTower(tempTower);
                        cell.enable(false);
                        LOG.info("Tower placed: {} at ({},{}), credits left={}", this.placingTowerType, cellX, cellY, this.context.getCredits());
                    } else {
                        LOG.info("Tower placement rejected: not enough credits for {} (need {}, have {})",
                                this.placingTowerType, this.placingTowerType.price, this.context.getCredits());
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
        return selected;
    }
}
