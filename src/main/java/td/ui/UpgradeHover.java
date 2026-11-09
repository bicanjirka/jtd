package td.ui;

import td.tower.Tower;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradePath;
import td.tower.upgrade.UpgradeRemoval;
import td.tower.upgrade.UpgradeState;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;

import java.util.List;

/** What a click on the hovered node would do: buy its path, or, on an owned node, downgrade it. */
sealed interface UpgradeHover {

    /** The path a left click buys, and how much cash it is short of; 0 when it can be paid. */
    record Buying(UpgradePath path, int shortBy) implements UpgradeHover {

        boolean affordable() {
            return this.shortBy == 0;
        }

        /** Whether a click buys it now. */
        boolean buys() {
            return this.path.isOpen() && this.affordable();
        }
    }

    /**
     * What a right-click takes away, what it refunds, which of what goes no wave has used (refunded in full) and which
     * locked-out nodes it opens again.
     */
    record Downgrading(UpgradeRemoval removal, int refund, List<UpgradeNode> unused, List<UpgradeNode> reopens)
            implements UpgradeHover {

        public Downgrading {
            unused = List.copyOf(unused);
            reopens = List.copyOf(reopens);
        }

        /** The part of the refund that gives back all that was paid. */
        int refundInFull() {
            return Math.min(this.refund, this.unused.stream().mapToInt(UpgradeNode::price).sum());
        }

        /** Whether all of what goes refunds in full, so there is nothing to ask. */
        boolean allInFull() {
            return this.unused.size() == this.removal.removed().size();
        }
    }

    static UpgradeHover of(UpgradeNode node, Tower tower, GameWorld world) {
        UpgradeTree tree = tower.upgradeTree();
        UpgradeState owned = tower.upgrades();
        if (!owned.owns(node.id())) {
            UpgradePath path = tree.planPath(node, tower, world);
            return new Buying(path, world.economy().shortOf(path.total()));
        }
        UpgradeRemoval removal = tree.removalFor(node, tower, world);
        return new Downgrading(removal, tower.refundFor(removal),
                removal.removed().stream().filter(tower.use()::isUnused).toList(),
                tree.newlyLockedOut(removal.kept(), owned));
    }
}
