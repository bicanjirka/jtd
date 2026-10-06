package td.enemy;

/** What a mob's walk tells the towers once it ends: what it was worth and where it went. */
public interface EnemyWalk {

    /** The bounty a kill pays, whether this walk ended in a kill or a leak. */
    int getBounty();

    /**
     * The mob's place in the order mobs went live, from {@link EnemyRoster#recordEntry()}: a tower
     * built after it entered compares it with {@link EnemyRoster#entries()}. {@code 0} for a mob
     * that never entered.
     */
    long entryOrdinal();

    /**
     * Whether the stretch of path this mob walked, from where it appeared to where it is now,
     * came within {@code radius} pixels of the point.
     */
    boolean walkedWithin(double x, double y, double radius);
}
