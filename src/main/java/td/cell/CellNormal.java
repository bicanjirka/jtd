package td.cell;

import td.tower.Tower;

/**
 * The {@link Cell} implementation. Owned by the EDT, where placement, hover and selling arrive, and
 * read by the game-loop thread when it builds a frame, so mutable fields are volatile. They are
 * independent, so a frame one pulse stale is still correct.
 */
public class CellNormal implements Cell {

    private final int x;
    private final int y;
    private volatile HighlightType highlight = HighlightType.NONE;
    private volatile float highlightRange = 0;
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

    public HighlightType getHighlight() {
        return this.highlight;
    }

    public void setHighlight(HighlightType highlight) {
        this.highlight = highlight;
    }

    public float getHighlightRange() {
        return this.highlightRange;
    }

    public void setHighlightRange(float range) {
        this.highlightRange = range;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    /** Sets buildability directly, bypassing the tower check. */
    public void enable(boolean b) {
        this.buildable = b;
    }

}
