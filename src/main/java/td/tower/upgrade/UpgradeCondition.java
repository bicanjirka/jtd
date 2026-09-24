package td.tower.upgrade;

import td.tower.Tower;
import td.util.GameWorld;

/**
 * Whether a node is available to a tower, evaluated on each call. Independent of price, which is
 * checked separately.
 * <p>
 * {@link #always()} is the identity. {@link #owns(String)} and {@link #slotEmpty(UpgradeSlot)}
 * compose with {@link #and} and {@link #or} into prerequisites and slot exclusivity.
 */
public interface UpgradeCondition {

    static UpgradeCondition always() {
        return AlwaysCondition.INSTANCE;
    }

    static UpgradeCondition owns(String nodeId) {
        return new OwnsNodeCondition(nodeId);
    }

    static UpgradeCondition slotEmpty(UpgradeSlot slot) {
        return new SlotEmptyCondition(slot);
    }

    boolean isSatisfied(Tower tower, GameWorld context);

    /** A short description of the gate, e.g. {@code "10 kills"}. */
    String describe();

    /** Progress toward the gate, e.g. {@code "7/10 kills"}. Defaults to {@link #describe()}. */
    default String progress(Tower tower, GameWorld context) {
        return this.describe();
    }

    default UpgradeCondition and(UpgradeCondition other) {
        return new AndCondition(this, other);
    }

    default UpgradeCondition or(UpgradeCondition other) {
        return new OrCondition(this, other);
    }
}
