package td.tower;

import td.stat.StatBreakdown;
import td.stat.StatTerm;

/**
 * One row of a tower's sheet: what it reads, its base, what changed it and what it is now.
 *
 * @param source the stat whose changes move this row, so a node that changes it previews here
 */
public record TowerStatLine(StatBreakdown breakdown, TowerStat source) {

    /** A row nothing changes. */
    public static TowerStatLine fixed(StatTerm stat, TowerStat source, float value) {
        return new TowerStatLine(StatBreakdown.unchanged(stat, value), source);
    }

    public StatTerm stat() {
        return this.breakdown.stat();
    }

    /** The tower's own value, before anything changed it. */
    public float base() {
        return this.breakdown.base();
    }

    /** The value it works with now. */
    public float current() {
        return this.breakdown.current();
    }
}
