package td.tower;

import td.enemy.EnemyWalk;
import td.tower.buff.TowerBuff;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeState;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;

import java.util.List;

/**
 * One tower on the board. Concrete types are reached only through {@link TowerVisitor}.
 * <p>
 * {@link #getX()}/{@link #getY()} are the tower's centre in board pixels, used for range;
 * {@link #getBoardX()}/{@link #getBoardY()} are its cell's top-left corner, used for drawing.
 */
public interface Tower {
    TowerFactory.Type getType();

    /** What this tower is called: its type's name, unless what it has become renames it. */
    default String displayName() {
        return this.getType().displayName();
    }

    void doTick(int gameTime);

    /** Start of the tower's turn: expires a timed buff and re-reads its disruption, republishing stats only on a change. */
    void beginTick(int gameTime);

    /** Whether an enemy's disruption weakens this tower right now. */
    boolean isDisrupted();

    <R> R accept(TowerVisitor<R> visitor);

    boolean isSelected();

    void setSelected(boolean selected);

    /** What the info panel shows: stats as authored and now, behaviours, kills, damage and upgrades. */
    TowerInspection inspect();

    int getSellPrice();

    /** Range in cells. */
    float getRange();

    /** Range in pixels, with buffs. */
    float getRangeReal();

    int getX();

    int getY();

    int getBoardX();

    int getBoardY();

    /**
     * The buff this tower gives {@code other}, or {@link TowerBuff#none()}. Buffs are computed from
     * where towers stand, never stored.
     */
    TowerBuff buffFor(Tower other);

    /** Recomputes current stats from the towers on the board and this tower's upgrades. */
    void recalculateStats();

    /**
     * Detaches this tower on sell or level teardown. A tower that subscribes to anything must
     * unsubscribe here, or it leaks into the next level.
     */
    void doCleanup();

    /** Total damage that actually landed. */
    long getDamageDealt();

    TowerExperience experience();

    /** Adds a finished walk's bounty to this tower's XP. */
    void earnXp(int bounty);

    /**
     * Whether a finished walk reached this tower: the mob went live after the tower was built and
     * passed within its range, disruption aside.
     */
    boolean reached(EnemyWalk walk);

    int getKillCount();

    /** {@link UpgradeTree#none()} for a tower without upgrades. */
    UpgradeTree upgradeTree();

    /** Owned upgrade nodes, as one snapshot. */
    UpgradeState upgrades();

    /** Unowned nodes whose prerequisites are met. */
    default List<UpgradeNode> offeredUpgrades(GameWorld context) {
        return this.upgradeTree().offered(this, context);
    }

    /**
     * Pays for {@code node} and adds it to the owned nodes. Returns {@code false} with no effect if
     * the node is not in this tree, is owned, is gated, or is unaffordable. It checks and charges
     * in one call, so don't check affordability first.
     */
    boolean buyUpgrade(UpgradeNode node);
}
