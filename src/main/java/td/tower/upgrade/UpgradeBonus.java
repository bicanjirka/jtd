package td.tower.upgrade;

import td.stat.StatModifier;
import td.stat.StatPreview;
import td.tower.TowerStat;

import java.util.Optional;

/**
 * One stat an upgrade node changes, and how.
 *
 * @param given   whether the node hands the change to the towers around it rather than its own tower
 * @param preview the row it moves on the tower that would buy it, before and after; empty once owned
 */
public record UpgradeBonus(TowerStat stat, StatModifier change, boolean given, Optional<StatPreview> preview) {

    public static UpgradeBonus of(TowerStat stat, StatModifier change, boolean given) {
        return new UpgradeBonus(stat, change, given, Optional.empty());
    }

    public UpgradeBonus withPreview(StatPreview preview) {
        return new UpgradeBonus(this.stat, this.change, this.given, Optional.of(preview));
    }
}
