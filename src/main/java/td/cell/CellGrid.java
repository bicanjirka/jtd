package td.cell;

import java.util.function.Consumer;

/**
 * A level's board of {@link Cell}s. Never hands out its backing array, only the queries callers
 * make. {@link #empty()} stands for "no level loaded".
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

    public static CellGrid empty() {
        return EMPTY;
    }

    /** A board of buildable cells, each at its top-left pixel corner for {@code scale}. */
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
     * @throws IndexOutOfBoundsException off the board; check {@link #contains} first if unsure
     */
    public Cell at(int x, int y) {
        if (!this.contains(x, y)) {
            throw new IndexOutOfBoundsException(
                    "Cell (" + x + "," + y + ") is outside a " + this.width + "x" + this.height + " board");
        }
        return this.cells[x][y];
    }

    /** Visits every cell, column by column. */
    public void forEach(Consumer<Cell> action) {
        for (Cell[] column : this.cells) {
            for (Cell cell : column) {
                action.accept(cell);
            }
        }
    }
}
