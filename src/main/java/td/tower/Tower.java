package td.tower;

import td.tower.buff.TowerBuff;
import td.tower.upgrade.UpgradePath;

import java.util.List;
import java.util.Optional;

/**
 * One tower on the board, as seen by the roster, the cell it occupies and the renderer.
 * Concrete types are reached only through {@link TowerVisitor}, never by casting.
 * <p>
 * {@link #getX()}/{@link #getY()} are the tower's <em>centre</em> in board pixels (what range
 * checks and beams use); {@link #getBoardX()}/{@link #getBoardY()} are its cell's top-left
 * corner (what drawing uses). They are not interchangeable.
 */
public interface Tower {
    TowerFactory.Type getType();

    void doTick(int gameTime);

    <R> R accept(TowerVisitor<R> visitor);

    boolean isSelected();

    void setSelected(boolean selected);

    /**
     * Pre-purchase blurb: base stats and price, as shown while hovering the toolbar.
     */
    String getInfoString();

    /**
     * Live blurb for an already-placed, selected tower: buffed stats plus kills and damage dealt.
     */
    String getStatusString();

    int getSellPrice();

    /**
     * Range in cells, as the tower's stats advertise it.
     */
    float getRange();

    /**
     * Range in pixels, buffs already applied - what an actual targeting query uses.
     */
    float getRangeReal();

    int getX();

    int getY();

    int getBoardX();

    int getBoardY();

    /** Attaches a nearby Aura tower's buff, recomputing this tower's damage and range. */
    /**
     * The buff this tower contributes to {@code other}, or {@link TowerBuff#none()} if it
     * contributes nothing - which is every tower that is not an Aura tower, and an Aura tower
     * that {@code other} is out of range of or that is itself.
     * <p>
     * Asking each tower what it gives is what removed the bidirectional index the buff system
     * used to keep: a {@code AuraTower} held its clients, every tower held its auras, and four
     * methods plus a listener plus a rescan kept the two sides in agreement. The buff a tower
     * receives is a pure function of where the towers are, so it is computed, not stored.
     */
    TowerBuff buffFor(Tower other);

    /**
     * Recomputes this tower's buffed damage/range/fire rate from the towers currently on the
     * board and its own chosen upgrade path. Called by {@code TowerRoster} whenever the set of
     * towers changes, and by {@code chooseUpgradePath}.
     */
    void recalculateStats();

    /**
     * Detaches this tower from everything it is wired into - buff partners, and any listener
     * hub it subscribed to. Called on sell and on level teardown; a tower that subscribes to
     * anything must unsubscribe here or it leaks into the next level.
     */
    void doCleanup();

    /**
     * Total damage this tower has actually landed - see AbstractTower.dealDamage's accounting rules.
     */
    long getDamageDealt();

    /**
     * How many kills this tower has landed - see AbstractTower.dealDamage's accounting rules.
     */
    int getKillCount();

    /**
     * This tower's specialization paths (exactly two, for v1) - empty for a tower that offers
     * none, like the Aura tower.
     */
    List<UpgradePath> availablePaths();

    /**
     * The path this tower has permanently specialized into, or {@code null} if it hasn't chosen one yet.
     */
    Optional<UpgradePath> getChosenPath();

    /**
     * Spends {@code path}'s price and permanently specializes this tower along it. Returns
     * {@code false} without effect if a path is already chosen, {@code path} isn't one of
     * this tower's own {@link #availablePaths()}, {@code path}'s own
     * {@code UpgradeCondition} isn't currently satisfied, or the player can't afford it -
     * mirrors {@code EconomyLedger.doPay}'s check-and-charge-in-one-call contract, so a caller
     * must not gate this on a separate affordability or condition check first.
     */
    boolean chooseUpgradePath(UpgradePath path);

}
