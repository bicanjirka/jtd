package td.tower.upgrade;

import java.util.List;

/**
 * What downgrading {@code node} takes away: the node and every owned node that depended on it, in purchase
 * order, and the nodes the tower keeps. Empty for a node the tower doesn't own.
 */
public record UpgradeRemoval(UpgradeNode node, List<UpgradeNode> removed, UpgradeState kept) {

    public UpgradeRemoval {
        removed = List.copyOf(removed);
    }

    public boolean isEmpty() {
        return this.removed.isEmpty();
    }

    /** What the removed nodes cost when they were bought. */
    public int paid() {
        return this.removed.stream().mapToInt(UpgradeNode::price).sum();
    }
}
