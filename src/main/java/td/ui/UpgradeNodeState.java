package td.ui;

import td.tower.Tower;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeState;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;

import java.util.Optional;

/**
 * How the Upgrades panel shows one node now, read once per refresh.
 *
 * @param ring     for a node waiting on XP or its gate, how far the tower is toward it, 0 to 1: XP first, then
 *                 the gate; 0 otherwise
 * @param sealedBy the Core step that keeps a sealed node closed; empty otherwise
 * @param unused   for an owned node, whether no wave has run with it yet, so it sells back in full
 */
record UpgradeNodeState(UpgradeNode node, Look look, float ring, Optional<UpgradeNode> sealedBy, boolean unused) {

    /** One look per state, checked in this order: the first that applies wins. */
    enum Look {
        OWNED,
        /** An exclusive pick closed it, or a node it needs. */
        LOCKED_OUT,
        /** A Core step the tower lacks keeps it closed. */
        SEALED,
        /** It needs the node before it in its line. */
        NEEDS_PREVIOUS,
        /** Its XP or its gate is unmet. */
        WAITING,
        SHORT_OF_CASH,
        BUYABLE;

        /** Whether its price shows under it: the next node of a line, an open special. */
        boolean showsPrice() {
            return this == WAITING || this == SHORT_OF_CASH || this == BUYABLE;
        }
    }

    static UpgradeNodeState of(UpgradeNode node, Tower tower, GameWorld world) {
        UpgradeTree tree = tower.upgradeTree();
        UpgradeState owned = tower.upgrades();
        if (owned.owns(node.id())) {
            return state(node, Look.OWNED, 0f, Optional.empty()).withUnused(tower.use().isUnused(node));
        }
        if (tree.isLockedOut(node, owned)) {
            return state(node, Look.LOCKED_OUT, 0f, Optional.empty());
        }
        Optional<UpgradeNode> sealedBy = tree.sealedBy(node, tower, world);
        if (sealedBy.isPresent()) {
            return state(node, Look.SEALED, 0f, sealedBy);
        }
        if (!tree.offers(node, tower, world)) {
            return state(node, Look.NEEDS_PREVIOUS, 0f, Optional.empty());
        }
        if (!node.gateMet(tower, world)) {
            float ring = node.xpMet(tower) ? node.gate().progressShare(tower, world)
                    : tower.experience().xp() / (float) node.xp();
            return state(node, Look.WAITING, ring, Optional.empty());
        }
        Look look = world.economy().canPay(node.price()) ? Look.BUYABLE : Look.SHORT_OF_CASH;
        return state(node, look, 0f, Optional.empty());
    }

    private static UpgradeNodeState state(UpgradeNode node, Look look, float ring, Optional<UpgradeNode> sealedBy) {
        return new UpgradeNodeState(node, look, ring, sealedBy, false);
    }

    private UpgradeNodeState withUnused(boolean next) {
        return new UpgradeNodeState(this.node, this.look, this.ring, this.sealedBy, next);
    }
}
