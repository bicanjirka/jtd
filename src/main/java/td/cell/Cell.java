package td.cell;

import td.tower.Tower;

/**
 * One square of the board: where it sits in pixels, whether a tower may be built on it, the
 * tower currently occupying it, and its transient placement/selection highlight.
 * <p>
 * Buildability has two independent sources - the level's path covers a cell
 * ({@code enable(false)} at load time, see {@link td.wave.PathCoverage}) or a tower occupies
 * it - and both funnel through the same flag, which is why selling a tower calls
 * {@code unSetTower()} followed by {@code enable(true)} rather than either one alone.
 */
public interface Cell {

    HighlightType getHighlight();

    void setHighlight(HighlightType highlight);

    float getHighlightRange();

    void setHighlightRange(float range);

    /**
     * The cell's top-left corner in board pixels, not its grid index.
     */
    int getX();

    int getY();

    void enable(boolean b);

    boolean buildable();

    boolean hasTower();

    void unSetTower();

    Tower getTower();

    void setTower(Tower tower);

    /**
     * {@code PLACE} is the hover highlight shown while placing a tower (with a range circle),
     * {@code SELECT} the outline on an already-placed tower's cell, {@code NONE} the resting
     * state - which is also the only one the renderer skips entirely.
     */
    enum HighlightType {
        NONE,
        SELECT,
        PLACE
    }

}
