package td.cell;

import td.tower.Tower;

public class CellNormal implements Cell {

    private highlightType highlight = highlightType.none;
    private float highlightRange = 0;

    private final int x;
    private final int y;

    private boolean buildable = true;
    private Tower tower = null;

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
        this.highlight = highlightType.none;
    }

    public Tower getTower() {
        return this.tower;
    }

    public void setTower(Tower tower) {
        if (this.buildable) {
            this.tower = tower;
            this.buildable = false;
        }
    }

    public boolean buildable() {
        return this.buildable;
    }

    public void setHighlight(highlightType highlight) {
        this.highlight = highlight;
    }

    public highlightType getHighlight() {
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

    public void enable(boolean b) {
        this.buildable = b;
    }

}
