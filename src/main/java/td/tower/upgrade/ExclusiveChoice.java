package td.tower.upgrade;

import java.util.Arrays;
import java.util.List;

/**
 * Nodes that exclude each other: a tower owns at most {@code picks} of them, and once it does the
 * rest are never offered again. The UI draws a choice as one linked set.
 */
public record ExclusiveChoice(List<String> nodeIds, int picks) {

    /** Rejects a choice of fewer than two nodes, or one allowing as many picks as it has nodes. */
    public ExclusiveChoice {
        nodeIds = List.copyOf(nodeIds);
        if (nodeIds.size() < 2 || picks < 1 || picks >= nodeIds.size()) {
            throw new IllegalArgumentException("A choice needs more nodes than picks: " + nodeIds + ", " + picks);
        }
    }

    /** Buying one locks the others out for good: the two chain roots, or IV-A and IV-B. */
    public static ExclusiveChoice oneOf(UpgradeNode... members) {
        return new ExclusiveChoice(ids(members), 1);
    }

    /** A tower's special set: one pick per special slot. */
    public static ExclusiveChoice specials(UpgradeNode... members) {
        return new ExclusiveChoice(ids(members), 2);
    }

    public boolean contains(String nodeId) {
        return this.nodeIds.contains(nodeId);
    }

    /** Whether {@code owned} has used every pick, which locks out the members it doesn't own. */
    boolean isSpent(UpgradeState owned) {
        return this.nodeIds.stream().filter(owned::owns).count() >= this.picks;
    }

    private static List<String> ids(UpgradeNode... members) {
        return Arrays.stream(members).map(UpgradeNode::id).toList();
    }
}
