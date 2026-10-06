package td;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import td.board.BoardGeometry;
import td.cell.Cell;
import td.cell.CellGrid;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.util.GameWorld;
import td.util.ThreadConfined;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Placement mode and the click highlight shared by placing and selecting. The grid is supplied,
 * since each level load replaces it.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
public class TowerPlacement {

    private static final Logger LOG = LoggerFactory.getLogger(TowerPlacement.class);

    private final GameWorld gameWorld;
    private final Supplier<CellGrid> cellGrid;

    private boolean placingTower = false;
    private TowerFactory.Type placingTowerType;
    private float placingTowerRange = 0;
    private int[] highlightedCell;

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
     * Drops placement mode and the highlight without touching the grid, for use before a possibly
     * smaller grid replaces it.
     */
    public void reset() {
        this.placingTower = false;
        this.placingTowerType = null;
        this.placingTowerRange = 0;
        this.highlightedCell = null;
    }

    private void unHighlightCell() {
        if (this.highlightedCell != null) {
            this.cellGrid.get().at(this.highlightedCell[0], this.highlightedCell[1]).setHighlight(Cell.HighlightType.NONE);
            this.highlightedCell = null;
        }
    }

    public void highlightCell(int boardX, int boardY) {
        this.unHighlightCell();
        BoardGeometry board = this.gameWorld.getBoard();
        if (board.containsPixel(boardX, boardY)) {
            this.highlightedCell = new int[]{board.cellX(boardX), board.cellY(boardY)};
            Cell cell = this.cellGrid.get().at(board.cellX(boardX), board.cellY(boardY));
            cell.setHighlight(Cell.HighlightType.PLACE);
            cell.setHighlightRange(this.placingTowerRange);
        }
    }

    /**
     * @return the tower selected by the click, or empty
     */
    public Optional<Tower> mouseClicked(int boardX, int boardY) {
        Tower selected = null;
        BoardGeometry board = this.gameWorld.getBoard();
        if (board.containsPixel(boardX, boardY)) {
            Cell cell = this.cellGrid.get().at(board.cellX(boardX), board.cellY(boardY));
            if (cell.hasTower()) {
                selected = cell.getTower();
                this.highlightedCell = new int[]{board.cellX(boardX), board.cellY(boardY)};
                cell.setHighlight(Cell.HighlightType.SELECT);
            } else if (this.placingTower) {
                if (cell.buildable()) {
                    int cellX = board.cellX(boardX);
                    int cellY = board.cellY(boardY);
                    int price = this.gameWorld.towers().priceOf(this.placingTowerType);
                    if (this.gameWorld.economy().doPay(price)) {
                        Tower tower = TowerFactory.createTower(this.placingTowerType, this.gameWorld, cellX, cellY);
                        this.gameWorld.towers().add(tower);
                        cell.setTower(tower);
                        cell.enable(false);
                        LOG.info("Tower placed: {} at ({},{}), credits left={}", this.placingTowerType, cellX, cellY, this.gameWorld.economy().getCredits());
                    } else {
                        LOG.info("Tower placement rejected: not enough credits for {} (need {}, have {})",
                                this.placingTowerType, price, this.gameWorld.economy().getCredits());
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
