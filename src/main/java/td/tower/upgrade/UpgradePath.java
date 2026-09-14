package td.tower.upgrade;

import td.tower.buff.TowerBuff;

/**
 * One of a tower's (exactly two, for v1) permanent specializations: a name shown in the UI,
 * its price (paid the same way buying a tower is - see {@code GameWorld.doPay}), the stat
 * bonus it grants via {@link TowerBuff}'s existing additive algebra, and the
 * {@link UpgradeCondition} gating it. Choosing one is a one-time, exclusive event handled by
 * {@code Tower.chooseUpgradePath} - this record is just the immutable description of the
 * choice, not the choice itself.
 */
public record UpgradePath(String displayName, int price, TowerBuff statBonus, UpgradeCondition condition) {
}
