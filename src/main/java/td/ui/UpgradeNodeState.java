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
 */
record UpgradeNodeState(UpgradeNode node, Look look, float ring, Optional<UpgradeNode> sealedBy) {

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
            return new UpgradeNodeState(node, Look.OWNED, 0f, Optional.empty());
        }
        if (tree.isLockedOut(node, owned)) {
            return new UpgradeNodeState(node, Look.LOCKED_OUT, 0f, Optional.empty());
        }
        Optional<UpgradeNode> sealedBy = tree.sealedBy(node, tower, world);
        if (sealedBy.isPresent()) {
            return new UpgradeNodeState(node, Look.SEALED, 0f, sealedBy);
        }
        if (!tree.offers(node, tower, world)) {
            return new UpgradeNodeState(node, Look.NEEDS_PREVIOUS, 0f, Optional.empty());
        }
        if (!node.gateMet(tower, world)) {
            float ring = node.xpMet(tower) ? node.gate().progressShare(tower, world)
                    : tower.experience().xp() / (float) node.xp();
            return new UpgradeNodeState(node, Look.WAITING, ring, Optional.empty());
        }
        Look look = world.economy().canPay(node.price()) ? Look.BUYABLE : Look.SHORT_OF_CASH;
        return new UpgradeNodeState(node, look, 0f, Optional.empty());
    }
}
