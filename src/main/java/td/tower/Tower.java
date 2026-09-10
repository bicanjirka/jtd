package td.tower;

/**
 * One tower on the board, as seen by the roster, the cell it occupies and the renderer.
 * Concrete types are reached only through {@link TowerVisitor}, never by casting.
 * <p>
 * {@link #getX()}/{@link #getY()} are the tower's <em>centre</em> in board pixels (what range
 * checks and beams use); {@link #getBoardX()}/{@link #getBoardY()} are its cell's top-left
 * corner (what drawing uses). They are not interchangeable.
 */
public interface Tower {
    TowerFactory.type getType();

    void doTick(int gameTime);

    <R> R accept(TowerVisitor<R> visitor);

    void setSelected(boolean selected);

    boolean isSelected();

    /** Pre-purchase blurb: base stats and price, as shown while hovering the toolbar. */
    String getInfoString();

    /** Live blurb for an already-placed, selected tower: buffed stats plus kills and damage dealt. */
    String getStatusString();

    int getSellPrice();

    /** Range in cells, as the tower's stats advertise it. */
    float getRange();

    /** Range in pixels, buffs already applied - what an actual targeting query uses. */
    float getRangeReal();

    int getX();

    int getY();

    int getBoardX();

    int getBoardY();

    /** Attaches a nearby upgrade tower's buff, recomputing this tower's damage and range. */
    void registerTower(Tower t);

    void unregisterTower(Tower t);

    /**
     * Detaches this tower from everything it is wired into - buff partners, and any listener
     * hub it subscribed to. Called on sell and on level teardown; a tower that subscribes to
     * anything must unsubscribe here or it leaks into the next level.
     */
    void doCleanup();

}
