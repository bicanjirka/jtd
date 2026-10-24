package td.tower.upgrade;

import td.term.TermText;
import td.tower.Tower;
import td.util.GameWorld;

import java.util.Set;
import java.util.function.Predicate;

/**
 * Whether a node is available to a tower, evaluated on each call. Independent of price, which is
 * checked separately.
 * <p>
 * {@link #always()} is the identity. {@link #owns(String)} and {@link #slotEmpty(UpgradeSlot)}
 * compose with {@link #and} and {@link #or} into prerequisites. Exclusivity between nodes is an
 * {@link ExclusiveChoice}, not a condition.
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

    /** Whether {@code tower} would meet this condition if it owned {@code owned} instead of what it owns. */
    boolean isSatisfied(Tower tower, UpgradeState owned, GameWorld context);

    default boolean isSatisfied(Tower tower, GameWorld context) {
        return this.isSatisfied(tower, tower.upgrades(), context);
    }

    /**
     * The ids of the nodes a tower must own to meet this condition, taking the cheaper side of an
     * {@code or}; none for a condition that asks about anything else. It is how a tool prices a node with
     * everything that leads to it.
     */
    default Set<String> requiredNodes() {
        return Set.of();
    }

    /**
     * The ids of the nodes {@code owned} still lacks for this condition: an {@code or} met on one side needs
     * nothing, else it takes the side buying can meet with fewer nodes. None for a condition no purchase
     * changes.
     */
    default Set<String> missingNodes(Tower tower, UpgradeState owned, GameWorld context) {
        return Set.of();
    }

    /**
     * Whether no purchase can ever meet this condition, given which nodes are locked out for good: an
     * {@code owns} of a locked node, an {@code and} with such a side, an {@code or} with both.
     */
    default boolean lockedOut(Predicate<String> nodeLockedOut) {
        return false;
    }

    /** A short description of the gate, e.g. {@code "10 kills"}. */
    TermText describe();

    /** Progress toward the gate, e.g. {@code "7/10 kills"}. Defaults to {@link #describe()}. */
    default TermText progress(Tower tower, GameWorld context) {
        return this.describe();
    }

    /** How far {@code tower} is toward this condition, 0 to 1, for a progress ring. Met or not by default. */
    default float progressShare(Tower tower, GameWorld context) {
        return this.isSatisfied(tower, context) ? 1f : 0f;
    }

    default UpgradeCondition and(UpgradeCondition other) {
        return new AndCondition(this, other);
    }

    default UpgradeCondition or(UpgradeCondition other) {
        return new OrCondition(this, other);
    }
}
