package td.cell;

import java.util.function.Consumer;

/**
 * A level's board of {@link Cell}s. Owns the grid outright and never hands it out: callers
 * get the queries they actually make - the cell at a position, the board's dimensions,
 * iteration - rather than the backing array.
 * <p>
 * That matters more for an array than for a collection, since an array cannot even be wrapped
 * in an unmodifiable view; returning one gives every caller unrestricted write access to this
 * class's internals and leaves it unable to hold any invariant over them.
 * <p>
 * {@link #empty()} is the "no level loaded" value, so nothing has to model that state as
 * {@code null} or guard against it before painting.
 */
public final class CellGrid {

    private static final CellGrid EMPTY = new CellGrid(new Cell[0][0]);

    private final Cell[][] cells;
    private final int width;
    private final int height;

    private CellGrid(Cell[][] cells) {
        this.cells = cells;
        this.width = cells.length;
        this.height = this.width == 0 ? 0 : cells[0].length;
    }

    /**
     * The board of a level that has not been loaded: no cells, and {@link #isLoaded()} false.
     */
    public static CellGrid empty() {
        return EMPTY;
    }

    /**
     * A fresh board of {@code width} x {@code height} buildable cells, each positioned at its
     * own top-left pixel corner for the given board {@code scale}.
     */
    public static CellGrid of(int width, int height, int scale) {
        if (width <= 0 || height <= 0) {
            return EMPTY;
        }
        Cell[][] cells = new Cell[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                cells[x][y] = new CellNormal(x * scale, y * scale);
            }
        }
        return new CellGrid(cells);
    }

    /**
     * Whether a level's board is actually loaded, as opposed to {@link #empty()}.
     */
    public boolean isLoaded() {
        return this.width > 0;
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    public boolean contains(int x, int y) {
        return x >= 0 && x < this.width && y >= 0 && y < this.height;
    }

    /**
     * The cell at grid coordinates {@code (x, y)}.
     *
     * @throws IndexOutOfBoundsException if the coordinates are off the board - callers that
     *                                   cannot rule that out first should ask {@link #contains}
     */
    public Cell at(int x, int y) {
        if (!this.contains(x, y)) {
            throw new IndexOutOfBoundsException(
                    "Cell (" + x + "," + y + ") is outside a " + this.width + "x" + this.height + " board");
        }
        return this.cells[x][y];
    }

    /**
     * Visits every cell, column by column. A no-op on {@link #empty()}.
     */
    public void forEach(Consumer<Cell> action) {
        for (Cell[] column : this.cells) {
            for (Cell cell : column) {
                action.accept(cell);
            }
        }
    }
}
