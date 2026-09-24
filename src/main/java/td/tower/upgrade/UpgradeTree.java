package td.tower.upgrade;

import td.tower.Tower;
import td.util.GameWorld;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * One tower type's upgrade nodes across all slots. {@link #offered(Tower, GameWorld)} is the query
 * every caller shares.
 */
public record UpgradeTree(List<UpgradeNode> nodes) {

    private static final UpgradeTree NONE = new UpgradeTree(List.of());

    /** Rejects duplicate node ids, which would make matching by id ambiguous. */
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
     * Unowned nodes whose prerequisites are met, by slot and then declaration order - the order the
     * upgrade panel and number keys use. A node whose prerequisite isn't met is absent, not
     * disabled.
     */
    public List<UpgradeNode> offered(Tower tower, GameWorld context) {
        return this.nodes.stream()
                .filter(n -> !tower.upgrades().owns(n.id()))
                .filter(n -> n.requires().isSatisfied(tower, context))
                .sorted(Comparator.comparing(UpgradeNode::slot))
                .toList();
    }
}
