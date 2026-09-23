package td.tower.upgrade;

import td.tower.Tower;
import td.util.GameWorld;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * One tower type's full set of upgrade nodes, across all three slots - the successor to a
 * leaf's old, flat {@code availablePaths()} list. {@link #offered(Tower, GameWorld)} is the one
 * query the sidebar panel, the number-key shortcut and the board's "ready" marker all share,
 * mirroring this codebase's "expose the queries callers make" convention (root {@code
 * CLAUDE.md} 6) rather than handing out the raw node list plus a prerequisite check each caller
 * re-derives.
 */
public record UpgradeTree(List<UpgradeNode> nodes) {

    private static final UpgradeTree NONE = new UpgradeTree(List.of());

    /**
     * Rejects a tree with two nodes sharing an id - {@code onUpgradeBought} and every
     * {@code UpgradeCondition.owns} reference match by id alone, so a duplicate would make
     * that match ambiguous.
     */
    public UpgradeTree {
        Set<String> ids = new HashSet<>();
        for (UpgradeNode node : nodes) {
            if (!ids.add(node.id())) {
                throw new IllegalArgumentException("Duplicate upgrade node id: " + node.id());
            }
        }
        nodes = List.copyOf(nodes);
    }

    public static UpgradeTree none() {
        return NONE;
    }

    public static UpgradeTree of(UpgradeNode... nodes) {
        return new UpgradeTree(List.of(nodes));
    }

    public List<UpgradeNode> nodesIn(UpgradeSlot slot) {
        return this.nodes.stream().filter(n -> n.slot() == slot).toList();
    }

    /**
     * The nodes this tower doesn't yet own whose {@code requires} is currently satisfied,
     * ordered by slot and then by this tree's own declaration order within a slot - the same
     * order the sidebar panel numbers its buttons in and the number-key shortcut reads from.
     * A node whose {@code requires} isn't met yet (the sibling of an already-chosen root, or a
     * level 2 before its level 1) is simply absent, not shown-and-disabled.
     */
    public List<UpgradeNode> offered(Tower tower, GameWorld context) {
        return this.nodes.stream()
                .filter(n -> !tower.upgrades().owns(n.id()))
                .filter(n -> n.requires().isSatisfied(tower, context))
                .sorted(Comparator.comparing(UpgradeNode::slot))
                .toList();
    }
}
