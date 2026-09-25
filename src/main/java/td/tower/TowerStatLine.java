package td.tower;

/**
 * One stat as authored and as it is now, after upgrades, auras and disruption.
 *
 * @param base    the tower's own value, before anything changed it
 * @param current the value it attacks with
 */
public record TowerStatLine(TowerStat stat, float base, float current) {

    /** A stat nothing changes. */
    public static TowerStatLine fixed(TowerStat stat, float value) {
        return new TowerStatLine(stat, value, value);
    }
}
