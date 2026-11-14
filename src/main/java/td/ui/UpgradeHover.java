package td.ui;

import td.economy.Refund;
import td.tower.Tower;
import td.tower.upgrade.UpgradeNode;
import td.tower.upgrade.UpgradePath;
import td.tower.upgrade.UpgradeRemoval;
import td.tower.upgrade.UpgradeState;
import td.tower.upgrade.UpgradeSwitch;
import td.tower.upgrade.UpgradeTree;
import td.util.GameWorld;

import java.util.List;
import java.util.Optional;

/**
 * What a click on the hovered node would do: buy its path, take it in its cluster's place, or, on an owned node,
 * downgrade it. A switch and a downgrade are {@code allowed} only between waves.
 */
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

    /** What a left click on a cluster's other member does: the member it replaces, and what is sold back with it. */
    record Switching(UpgradeSwitch change, Refund refund, boolean allowed) implements UpgradeHover {
    }

    /** What a right-click takes away, what it refunds and which locked-out nodes it opens again. */
    record Downgrading(UpgradeRemoval removal, Refund refund, List<UpgradeNode> reopens, boolean allowed)
            implements UpgradeHover {

        public Downgrading {
            reopens = List.copyOf(reopens);
        }
    }

    static UpgradeHover of(UpgradeNode node, Tower tower, GameWorld world) {
        UpgradeTree tree = tower.upgradeTree();
        UpgradeState owned = tower.upgrades();
        boolean allowed = UpgradeTree.changesAllowed(world);
        if (!owned.owns(node.id())) {
            Optional<UpgradeSwitch> change = tree.switchFor(node, tower, world);
            if (change.isPresent()) {
                return new Switching(change.get(), tower.refundFor(change.get()), allowed);
            }
            UpgradePath path = tree.planPath(node, tower, world);
            return new Buying(path, world.economy().shortOf(path.total()));
        }
        UpgradeRemoval removal = tree.removalFor(node, tower, world);
        return new Downgrading(removal, tower.refundFor(removal), tree.newlyLockedOut(removal.kept(), owned), allowed);
    }
}
