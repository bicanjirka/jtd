package td.cell;

/**
 * The one highlighted cell, by grid index: the placement hover with the range and price of the tower it would
 * build, or the selected tower's outline. Published whole, so a frame never mixes two hovers.
 */
public record CellHighlight(Kind kind, int cellX, int cellY, float rangeCells, int price) {

    private static final CellHighlight NONE = new CellHighlight(Kind.NONE, 0, 0, 0f, 0);

    public static CellHighlight none() {
        return NONE;
    }

    public static CellHighlight place(int cellX, int cellY, float rangeCells, int price) {
        return new CellHighlight(Kind.PLACE, cellX, cellY, rangeCells, price);
    }

    public static CellHighlight select(int cellX, int cellY) {
        return new CellHighlight(Kind.SELECT, cellX, cellY, 0f, 0);
    }

    public boolean isOn(int x, int y) {
        return this.kind != Kind.NONE && this.cellX == x && this.cellY == y;
    }

    /**
     * {@code PLACE} is the placement hover, {@code SELECT} the selected tower's outline,
     * {@code NONE} draws nothing.
     */
    public enum Kind {
        NONE,
        SELECT,
        PLACE
    }
}
