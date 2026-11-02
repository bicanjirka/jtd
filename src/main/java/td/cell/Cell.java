package td.cell;

import td.tower.Tower;

/**
 * One board square: pixel position, buildability and occupying tower. The highlight is not the cell's:
 * it is one {@link CellHighlight} for the whole board.
 * <p>
 * A path covering the cell and a tower occupying it share one buildability flag, so selling calls
 * {@code unSetTower()} and then {@code setBuildable(true)}.
 * <p>
 * Owned by the EDT, where placement and selling arrive, and read by the game-loop thread when it builds a
 * frame, so mutable fields are volatile. They are independent, so a frame one pulse stale is still correct.
 */
public final class Cell {

    private final int x;
    private final int y;
    private volatile boolean buildable = true;
    private volatile Tower tower = null;

    public Cell(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public boolean hasTower() {
        return (this.tower != null);
    }

    public void unSetTower() {
        if (this.tower != null) {
            this.tower = null;
            this.buildable = true;
        }
    }

    public Tower getTower() {
        return this.tower;
    }

    /** Does nothing on an unbuildable cell. */
    public void setTower(Tower tower) {
        if (this.buildable) {
            this.tower = tower;
            this.buildable = false;
        }
    }

    public boolean buildable() {
        return this.buildable;
    }

    /** Top-left corner in board pixels, not the grid index. */
    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    /** Sets buildability directly, bypassing the tower check. */
    public void setBuildable(boolean buildable) {
        this.buildable = buildable;
    }

}
