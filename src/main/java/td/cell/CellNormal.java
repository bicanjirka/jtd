package td.cell;

import td.tower.Tower;

/**
 * The only {@link Cell} implementation. Position is fixed at construction; everything else is
 * mutable board state.
 * <p>
 * A cell is owned by the Event Dispatch Thread - placement, hover and selling all arrive as
 * input events - but it is read by the {@code game-loop} thread, which walks the grid when it
 * builds a render frame. The mutable fields are therefore published volatile. Each is an
 * independent value with no invariant tying it to the others, so a frame that catches a
 * highlight one pulse late is correct, just momentarily stale. See CLAUDE.md 3.
 */
public class CellNormal implements Cell {

    private volatile HighlightType highlight = HighlightType.NONE;
    private volatile float highlightRange = 0;

    private final int x;
    private final int y;

    private volatile boolean buildable = true;
    private volatile Tower tower = null;

    public CellNormal(int x, int y) {
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
        this.highlight = HighlightType.NONE;
    }

    public Tower getTower() {
        return this.tower;
    }

    /** Silently does nothing on an unbuildable cell - placement is gated before it gets here. */
    public void setTower(Tower tower) {
        if (this.buildable) {
            this.tower = tower;
            this.buildable = false;
        }
    }

    public boolean buildable() {
        return this.buildable;
    }

    public void setHighlight(HighlightType highlight) {
        this.highlight = highlight;
    }

    public HighlightType getHighlight() {
        return this.highlight;
    }

    public void setHighlightRange(float range) {
        this.highlightRange = range;
    }

    public float getHighlightRange() {
        return this.highlightRange;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    /**
     * Sets buildability directly, bypassing the tower check - used at level load to mark the
     * cells the path covers, and again when a sold tower frees its cell.
     */
    public void enable(boolean b) {
        this.buildable = b;
    }

}
