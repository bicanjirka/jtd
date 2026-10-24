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

    /** The path a left click buys, and the credits there are to pay for it. */
    record Buying(UpgradePath path, int credits) implements UpgradeHover {

        boolean affordable() {
            return this.credits >= this.path.total();
        }

        /** Whether a click buys it now. */
        boolean buys() {
            return this.path.isOpen() && this.affordable();
        }
    }

    /** What a right-click takes away, what it refunds and which locked-out nodes it opens again. */
    record Downgrading(UpgradeRemoval removal, int refund, List<UpgradeNode> reopens) implements UpgradeHover {

        public Downgrading {
            reopens = List.copyOf(reopens);
        }
    }

    static UpgradeHover of(UpgradeNode node, Tower tower, GameWorld world) {
        UpgradeTree tree = tower.upgradeTree();
        UpgradeState owned = tower.upgrades();
        if (!owned.owns(node.id())) {
            return new Buying(tree.planPath(node, tower, world), world.economy().getCredits());
        }
        UpgradeRemoval removal = tree.removalFor(node, tower, world);
        return new Downgrading(removal, tower.refundFor(removal), tree.newlyLockedOut(removal.kept(), owned));
    }
}
