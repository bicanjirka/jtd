package td.cell;

import td.tower.Tower;

/**
 * One board square: pixel position, buildability, occupying tower and highlight.
 * <p>
 * A path covering the cell and a tower occupying it share one buildability flag, so selling calls
 * {@code unSetTower()} and then {@code enable(true)}.
 */
public interface Cell {

    HighlightType getHighlight();

    void setHighlight(HighlightType highlight);

    float getHighlightRange();

    void setHighlightRange(float range);

    /** Top-left corner in board pixels, not the grid index. */
    int getX();

    int getY();

    void enable(boolean b);

    boolean buildable();

    boolean hasTower();

    void unSetTower();

    Tower getTower();

    void setTower(Tower tower);

    /**
     * {@code PLACE} is the placement hover, {@code SELECT} the selected tower's outline,
     * {@code NONE} draws nothing.
     */
    enum HighlightType {
        NONE,
        SELECT,
        PLACE
    }

}
