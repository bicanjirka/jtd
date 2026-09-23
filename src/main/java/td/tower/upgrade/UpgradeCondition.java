package td.tower.upgrade;

import td.tower.Tower;
import td.util.GameWorld;

/**
 * Whether an upgrade node is currently available to a specific tower - the gate a player
 * must clear before spending on it, evaluated fresh every time it's asked rather than
 * cached. This is independent of affordability: a caller checks both
 * {@code isSatisfied(...)} and the node's price separately, the same way the toolbar
 * already separates "is this tower type unlocked" from "can I afford it".
 * <p>
 * {@link #always()} is the identity - satisfied unconditionally - which is what a node
 * gated on money alone uses: affordability is the only real gate it has. {@link #owns(String)}
 * and {@link #slotEmpty(UpgradeSlot)} are the two structural checks a node's {@code requires}
 * composes ({@link #and}/{@link #or}) to express prerequisites and slot exclusivity - see
 * {@code StandardBaseSlot.opens}.
 */
public interface UpgradeCondition {

    static UpgradeCondition always() {
        return AlwaysCondition.INSTANCE;
    }

    /**
     * Satisfied once the tower already owns the node with this id, anywhere in its tree.
     */
    static UpgradeCondition owns(String nodeId) {
        return new OwnsNodeCondition(nodeId);
    }

    /**
     * Satisfied while nothing has been bought yet in {@code slot} - what makes two root nodes
     * of the same slot mutually exclusive once either one is chosen.
     */
    static UpgradeCondition slotEmpty(UpgradeSlot slot) {
        return new SlotEmptyCondition(slot);
    }

    boolean isSatisfied(Tower tower, GameWorld context);

    /**
     * A short, human-readable gate description - what the info panel shows next to an upgrade
     * node's name, e.g. {@code "10 kills"} or {@code "money only"}.
     */
    String describe();

    /**
     * A short, human-readable progress readout toward this gate, e.g. {@code "7/10 kills"} -
     * what the selected tower's status text shows for a node that's offered but not yet
     * satisfied. Defaults to {@link #describe()} for a gate with no meaningful partial
     * progress (money only, a structural prerequisite); {@code KillCountCondition},
     * {@code DamageDealtCondition} and {@code ClusterCondition} override it.
     */
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
