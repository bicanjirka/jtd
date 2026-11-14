package td.tower.upgrade;

import java.util.List;

/** One place in a tier's row, left to right: a node of its own, a cluster's frame, or the branches a cluster opens. */
public sealed interface RowPlace {

    /** A node in no cluster of this row: an orphan, or a step of a line. */
    record Lone(UpgradeNode node) implements RowPlace {
    }

    /** A cluster's members, drawn in one frame. */
    record Group(UpgradeCluster cluster, List<UpgradeNode> members) implements RowPlace {

        public Group {
            members = List.copyOf(members);
        }
    }

    /**
     * What comes after each member of {@code cluster} in this row, one branch per member in the cluster's order. Only
     * the owned member's branch shows, so the branches share one place, as wide as the widest.
     */
    record Branches(UpgradeCluster cluster, List<List<RowPlace>> branches) implements RowPlace {

        public Branches {
            branches = branches.stream().map(List::copyOf).toList();
        }
    }
}
