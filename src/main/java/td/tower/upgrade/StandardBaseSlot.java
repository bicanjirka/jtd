package td.tower.upgrade;

import td.tower.buff.TowerBuff;

/**
 * The {@code BASE} slot every tower shares: a range node and an awaken node that unlocks
 * {@code HEAD} and {@code SPECIAL}. Both are bought independently, gated on price alone.
 */
public final class StandardBaseSlot {

    public static final String RANGE_ID = "base.range";
    public static final String AWAKEN_ID = "base.awaken";

    private static final float RANGE_BONUS = 0.15f;

    private StandardBaseSlot() {
    }

    public static UpgradeNode rangeNode(int price) {
        return UpgradeNode.of(RANGE_ID, UpgradeSlot.BASE, "Range", price)
                .withBuff(TowerBuff.range(RANGE_BONUS));
    }

    public static UpgradeNode awakenNode(int price) {
        return UpgradeNode.of(AWAKEN_ID, UpgradeSlot.BASE, "Awaken", price)
                .withExtraEffect("unlocks head and special");
    }

    /**
     * The prerequisite for a {@code HEAD} or {@code SPECIAL} root: awaken owned and {@code slot}
     * still empty, which makes the roots mutually exclusive.
     */
    public static UpgradeCondition opens(UpgradeSlot slot) {
        return UpgradeCondition.owns(AWAKEN_ID).and(UpgradeCondition.slotEmpty(slot));
    }
}
