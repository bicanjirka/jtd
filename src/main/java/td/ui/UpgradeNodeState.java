package td.ui;

import td.tower.Tower;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradeState;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;

/**
 * How the Upgrades panel shows one node now, read once per refresh.
 *
 * @param ability whether it adds something the tower couldn't do, rather than changing a number: drawn round
 * @param unused  for an owned node, whether no wave has run with it yet, so it sells back in full
 */
record UpgradeNodeState(UpgradeNode node, Look look, boolean ability, boolean unused) {

    UpgradeNodeState {
        if (unused && look != Look.OWNED) {
            throw new IllegalArgumentException("Only an owned node can be unused: " + node.id() + " is " + look);
        }
    }

    /** One look per state, checked in this order: the first that applies wins. */
    enum Look {
        OWNED,
        /** Another member of its cluster is owned: a click takes this one instead. */
        SWITCHABLE,
        /** Its tier isn't open: the XP is short, or the tier's unlock isn't bought. */
        TIER_LOCKED,
        /** A node it needs can no longer be bought. */
        LOCKED_OUT,
        /** It needs a node the tower doesn't own yet. */
        NEEDS_PREVIOUS,
        SHORT_OF_CASH,
        BUYABLE;

        /** Whether its price shows under it. */
        boolean showsPrice() {
            return this == TIER_LOCKED || this == NEEDS_PREVIOUS || this == SHORT_OF_CASH || this == BUYABLE;
        }
    }

    static UpgradeNodeState of(UpgradeNode node, Tower tower, GameWorld world) {
        UpgradeTree tree = tower.upgradeTree();
        UpgradeState owned = tower.upgrades();
        boolean ability = tree.tierNodeOf(node.tier()).filter(node::equals).isEmpty()
                && !tower.wordsOf(node).sentences().isEmpty();
        Look look = tree.isGranted(node)
                ? tree.isHeld(node, tower.experience().xp(), owned) ? Look.OWNED : Look.TIER_LOCKED
                : lookOf(node, tower, world, tree, owned);
        return new UpgradeNodeState(node, look, ability,
                look == Look.OWNED && !tree.isGranted(node) && tower.use().isUnused(node));
    }

    private static Look lookOf(UpgradeNode node, Tower tower, GameWorld world, UpgradeTree tree, UpgradeState owned) {
        if (owned.owns(node.id())) {
            return Look.OWNED;
        }
        if (tree.switchFor(node, tower, world).isPresent()) {
            return Look.SWITCHABLE;
        }
        if (!tierReached(node, tower, world, tree)) {
            return Look.TIER_LOCKED;
        }
        if (tree.isLockedOut(node, owned)) {
            return Look.LOCKED_OUT;
        }
        if (!tree.offers(node, tower, world)) {
            return Look.NEEDS_PREVIOUS;
        }
        return world.economy().canPay(node.price()) ? Look.BUYABLE : Look.SHORT_OF_CASH;
    }

    /** A tier's unlock stands at the tier's gate, reached with the tier below and the XP. */
    private static boolean tierReached(UpgradeNode node, Tower tower, GameWorld world, UpgradeTree tree) {
        if (tree.isUnlock(node)) {
            return tree.isOpen(node.tier() - 1, tower, world) && tree.xpMet(node.tier(), tower, world);
        }
        return tree.isOpen(node.tier(), tower, world);
    }
}
