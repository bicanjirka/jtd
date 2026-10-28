package td.cell;

import td.tower.Tower;

/**
 * One board square: pixel position, buildability and occupying tower. The highlight is not the cell's:
 * it is one {@link CellHighlight} for the whole board.
 * <p>
 * A path covering the cell and a tower occupying it share one buildability flag, so selling calls
 * {@code unSetTower()} and then {@code enable(true)}.
 */
public interface Cell {

    /** Top-left corner in board pixels, not the grid index. */
    int getX();

    int getY();

    void enable(boolean b);

    boolean buildable();

    boolean hasTower();

    void unSetTower();

    Tower getTower();

    void setTower(Tower tower);

}
