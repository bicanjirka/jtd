package td.tower.upgrade;

import td.tower.buff.TowerBuff;

/**
 * The {@code BASE} slot's shape is identical on every tower: a range node and an Awaken node
 * that unlocks {@code HEAD}/{@code SPECIAL} for purchase. Both are buyable independently and
 * gated on price alone ({@code always()}) - {@code BASE} is the one slot that isn't exclusive,
 * since a tower "grows into" its other slots by investment rather than by performance. Defined
 * once here rather than per leaf, since every tower's base slot reads the same.
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
     * The shared {@code requires} for a {@code HEAD}/{@code SPECIAL} root node: Awaken must be
     * owned, and nothing has been chosen in {@code slot} yet. The second half is what makes a
     * slot's own root nodes mutually exclusive the moment either one is bought - not a separate
     * "already chose the other root" check per node.
     */
    public static UpgradeCondition opens(UpgradeSlot slot) {
        return UpgradeCondition.owns(AWAKEN_ID).and(UpgradeCondition.slotEmpty(slot));
    }
}
