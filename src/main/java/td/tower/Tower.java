package td.tower;

import td.damage.AttackOrigin;
import td.economy.Refund;
import td.enemy.EnemyMark;
import td.enemy.EnemyMob;
import td.enemy.EnemyWalk;
import td.stat.StatModifiers;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeRemoval;
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

    /**
     * Game loop, when the wave's last enemy is gone: ends everything the wave built up in this tower. Its turret
     * keeps its pose and turns to rest over the next ticks.
     */
    void endWave();

    /** Game loop, as a wave starts: {@link #endWave()}, with the turret at rest, so every wave starts alike. */
    void readyForWave();

    /**
     * Start of the tower's turn: expires its timed statuses and re-reads its disruption, republishing stats only on a
     * change.
     *
     * @return whether this tower republished its stats
     */
    boolean beginTick(int gameTime);

    /** Whether an enemy's disruption weakens this tower right now. */
    boolean isDisrupted();

    <R> R accept(TowerVisitor<R> visitor);

    boolean isSelected();

    void setSelected(boolean selected);

    /** What the info panel shows: stats as authored and now, behaviours, kills, damage and upgrades. */
    TowerInspection inspect();

    /** What selling it pays back now. */
    Refund sellRefund();

    /** What selling it pays back now, in all. */
    default int getSellPrice() {
        return this.sellRefund().total();
    }

    /** Range in cells, before upgrades and buffs. */
    float baseRange();

    /** Range in pixels, with buffs. */
    float getRangeReal();

    int getX();

    int getY();

    int getBoardX();

    /** Who this tower's hits and effects come from, for its whole life. */
    AttackOrigin origin();

    int getBoardY();

    /**
     * What this tower gives {@code other}, or {@link StatModifiers#none()}. Buffs are computed from
     * where towers stand, never stored.
     */
    StatModifiers<TowerStat> buffFor(Tower other);

    /**
     * Whether {@link #buffFor}, {@link #xpBonusFor} or {@link #xpSharedWith} can give anything, so a change to this
     * tower's own stats can change what others receive.
     */
    default boolean emitsBuffs() {
        return false;
    }

    /** Grants this tower {@code status}, as it would grant it to itself. */
    void receiveStatus(TowerStatus status);

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

    /** Adds the bounty plus {@code bonus} of it to this tower's XP; the fraction XP cannot hold carries to the next. */
    void earnXp(int bounty, float bonus);

    /**
     * The share of a bounty this tower adds to the XP {@code earner} earns from it, derived from where
     * towers stand like a buff. Nothing by default, and nothing from a tower that does not {@link #emitsBuffs}.
     */
    default float xpBonusFor(Tower earner) {
        return 0f;
    }

    /**
     * The towers this one makes earn what {@code earners} earn, besides them. None by default, and none from a
     * tower that does not {@link #emitsBuffs}.
     */
    default List<Tower> xpSharedWith(List<Tower> earners) {
        return List.of();
    }

    /**
     * Whether a finished walk reached this tower: the mob went live after the tower was built and
     * passed within its range, disruption aside.
     */
    boolean reached(EnemyWalk walk);

    int getKillCount();

    /** What it carries into the next wave: what it earned, what it was bought for, what of it is used. */
    TowerRecord record();

    /** Takes up {@code record} in place of its own: for a tower just rebuilt from a saved game. */
    void restore(TowerRecord record);

    /** What of it has been used, and so what sells back in full. */
    TowerUse use();

    /** Every buff and debuff this tower carries now, buffs first. Game loop only. */
    default List<TowerBuffState> conditions() {
        return List.of();
    }

    /** Memory this tower keeps about {@code enemy}, named for the player. Game loop only. */
    default List<EnemyMark> marksOn(EnemyMob enemy) {
        return List.of();
    }

    /** Every effect this tower puts on enemies or gives towers, with what applies it. Game loop only. */
    default List<AppliedEffect> applies() {
        return List.of();
    }

    /** How far each counting node it owns is toward its next go. Game loop only. */
    default List<NodeProgress> progress() {
        return List.of();
    }

    /** {@link UpgradeTree#none()} for a tower without upgrades. */
    UpgradeTree upgradeTree();

    /** Owned upgrade nodes, as one snapshot. */
    UpgradeState upgrades();

    /** What {@code node} says to the player: its value rows and its sentences. */
    NodeWords wordsOf(UpgradeNode node);

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

    /**
     * Buys {@code target} with every node it needs first ({@link UpgradeTree#planPath}), paying the total
     * in one charge. Returns {@code false} with no effect if any step is blocked or the total is
     * unaffordable.
     */
    boolean buyPath(UpgradeNode target);

    /**
     * What a downgrade that takes {@code removal} away gives back: what selling would pay less, so that
     * downgrading and then selling pays what selling would have.
     */
    Refund refundFor(UpgradeRemoval removal);

    /**
     * Removes {@code node} and every owned node that depends on it ({@link UpgradeTree#removalFor}), and
     * refunds {@link #refundFor} them. Returns {@code false} with no effect for a node it doesn't own.
     */
    boolean downgrade(UpgradeNode node);
}
