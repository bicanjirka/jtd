package td;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import td.board.BoardGeometry;
import td.cell.Cell;
import td.cell.CellGrid;
import td.cell.CellHighlight;
import td.tower.Tower;
import td.tower.TowerFactory;
import td.util.GameWorld;
import td.util.ThreadConfined;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Placement mode and the click highlight shared by placing and selecting. The grid is supplied,
 * since each level load replaces it. The highlight is published whole for the frame build to read.
 */
@ThreadConfined(value = ThreadConfined.Owner.EVENT_DISPATCH_THREAD)
public class TowerPlacement {

    private static final Logger LOG = LoggerFactory.getLogger(TowerPlacement.class);

    private final GameWorld gameWorld;
    private final Supplier<CellGrid> cellGrid;

    private Optional<Placing> placing = Optional.empty();
    private volatile CellHighlight highlight = CellHighlight.none();
    private boolean pinningLeyLine = false;

    public TowerPlacement(GameWorld gameWorld, Supplier<CellGrid> cellGrid) {
        this.gameWorld = gameWorld;
        this.cellGrid = cellGrid;
    }

    public boolean isPlacing() {
        return this.placing.isPresent();
    }

    public void start(TowerFactory.Type t, float r) {
        this.placing = Optional.of(new Placing(t, r));
    }

    public void cancel() {
        this.placing = Optional.empty();
        this.unHighlightCell();
    }

    public void unSelectTower() {
        this.unHighlightCell();
    }

    /** Whether the next board click picks a Ley Line's host rather than selecting or building. */
    public boolean isPinningLeyLine() {
        return this.pinningLeyLine;
    }

    public void startPinningLeyLine() {
        this.pinningLeyLine = true;
    }

    public void stopPinningLeyLine() {
        this.pinningLeyLine = false;
    }

    /** Any thread: the highlighted cell as last published. */
    public CellHighlight highlight() {
        return this.highlight;
    }

    /** Drops the highlight if it is on the cell at grid ({@code x}, {@code y}), whose tower just went. */
    public void cellCleared(int x, int y) {
        if (this.highlight.isOn(x, y)) {
            this.unHighlightCell();
        }
    }

    /** The tower standing on the cell under this board pixel; empty off the board or on an empty cell. */
    public Optional<Tower> towerAt(int boardX, int boardY) {
        BoardGeometry board = this.gameWorld.getBoard();
        if (!board.containsPixel(boardX, boardY)) {
            return Optional.empty();
        }
        Cell cell = this.cellGrid.get().at(board.cellX(boardX), board.cellY(boardY));
        return cell.hasTower() ? Optional.of(cell.getTower()) : Optional.empty();
    }

    /** Drops placement mode and the highlight, for use before a possibly smaller grid replaces the grid. */
    public void reset() {
        this.placing = Optional.empty();
        this.pinningLeyLine = false;
        this.unHighlightCell();
    }

    private void unHighlightCell() {
        this.highlight = CellHighlight.none();
    }

    public void highlightCell(int boardX, int boardY) {
        BoardGeometry board = this.gameWorld.getBoard();
        if (board.containsPixel(boardX, boardY)) {
            int price = this.placing.map(p -> this.gameWorld.towers().priceOf(p.type())).orElse(0);
            float range = this.placing.map(Placing::range).orElse(0f);
            this.highlight = CellHighlight.place(board.cellX(boardX), board.cellY(boardY), range, price);
        } else {
            this.unHighlightCell();
        }
    }

    /**
     * A click while placing builds on a free cell, or selects the tower already there. Placing ends with it, unless
     * the cash was short: then placing goes on, so the click after the cash comes in builds.
     *
     * @return the tower selected by the click: the one on the clicked cell, or the one it built; else empty
     */
    public Optional<Tower> mouseClicked(int boardX, int boardY) {
        Tower selected = null;
        boolean shortOfCash = false;
        BoardGeometry board = this.gameWorld.getBoard();
        if (board.containsPixel(boardX, boardY)) {
            Cell cell = this.cellGrid.get().at(board.cellX(boardX), board.cellY(boardY));
            if (cell.hasTower()) {
                selected = cell.getTower();
                this.highlight = CellHighlight.select(board.cellX(boardX), board.cellY(boardY));
            } else if (this.placing.isPresent()) {
                TowerFactory.Type type = this.placing.orElseThrow().type();
                if (cell.buildable()) {
                    int cellX = board.cellX(boardX);
                    int cellY = board.cellY(boardY);
                    int price = this.gameWorld.towers().priceOf(type);
                    if (this.gameWorld.economy().doPay(price)) {
                        Tower tower = TowerFactory.createTower(type, this.gameWorld, cellX, cellY);
                        this.gameWorld.towers().add(tower);
                        cell.setTower(tower);
                        cell.setBuildable(false);
                        selected = tower;
                        this.highlight = CellHighlight.select(cellX, cellY);
                        LOG.info("Tower placed: {} at ({},{}), credits left={}", type, cellX, cellY, this.gameWorld.economy().getCredits());
                    } else {
                        LOG.info("Tower placement rejected: not enough credits for {} (need {}, have {})",
                                type, price, this.gameWorld.economy().getCredits());
                        shortOfCash = true;
                    }
                } else {
                    LOG.info("Tower placement rejected: cell ({},{}) is not buildable", board.cellX(boardX), board.cellY(boardY));
                }
            }
        }
        if (!shortOfCash) {
            this.placing = Optional.empty();
        }
        return Optional.ofNullable(selected);
    }

    /** The tower being placed and its range, which only mean something together. */
    private record Placing(TowerFactory.Type type, float range) {
    }
}
