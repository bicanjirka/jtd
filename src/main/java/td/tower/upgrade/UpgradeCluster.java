package td.tower.upgrade;

import java.util.Arrays;
import java.util.List;

/**
 * Upgrades drawn together in one frame and bought together: buying the cluster takes one member, which is the active
 * one, and the tower may switch to another for free between waves. {@code name} says what the members share ("On
 * crit"). A tower's fork is a cluster too, drawn in the gate below its tier.
 */
public record UpgradeCluster(String name, List<String> memberIds) {

    public UpgradeCluster {
        memberIds = List.copyOf(memberIds);
        if (memberIds.size() < 2) {
            throw new IllegalArgumentException("A cluster needs two members or more: " + name + " " + memberIds);
        }
    }

    public static UpgradeCluster of(String name, UpgradeNode... members) {
        return new UpgradeCluster(name, Arrays.stream(members).map(UpgradeNode::id).toList());
    }

    public boolean contains(String nodeId) {
        return this.memberIds.contains(nodeId);
    }

    /** Whether {@code owned} has a member: the cluster is paid, and its other members wait for a switch. */
    boolean isTaken(UpgradeState owned) {
        for (int i = 0; i < this.memberIds.size(); i++) {
            if (owned.owns(this.memberIds.get(i))) {
                return true;
            }
        }
        return false;
    }
}
