package td.tower.upgrade;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * What buying {@code target} with everything it needs first would take: the steps in the order they
 * are bought ({@code target} last), the first thing that stops it, and the nodes it would lock out. Bought whole
 * or not at all.
 */
public record UpgradePath(UpgradeNode target, List<UpgradeNode> steps, Optional<Block> block,
                          List<UpgradeNode> locksOut) {

    /** Why a path can't be bought now. */
    public enum Reason {
        /** The target is owned already. */
        OWNED,
        /** Another member of the step's cluster is the active one. */
        LOCKED_OUT,
        /** The step is a cluster's pick off the target's own line: the player makes that choice first. */
        CHOOSE_FIRST,
        /** Nothing on the way opens the step. */
        UNREACHABLE,
        /** The tower lacks the XP of the step's tier. */
        XP
    }

    /**
     * What stops a path, and at which of its steps (or the node it needed). {@code orElse} holds the other nodes that
     * would meet the same either-or requirement instead of {@code at}.
     */
    public record Block(UpgradeNode at, Reason reason, List<UpgradeNode> orElse) {

        public Block {
            orElse = List.copyOf(orElse);
        }

        public Block(UpgradeNode at, Reason reason) {
            this(at, reason, List.of());
        }

        /** {@code at} and each node that would do instead, in that order. */
        public List<UpgradeNode> choices() {
            List<UpgradeNode> choices = new ArrayList<>(List.of(this.at));
            choices.addAll(this.orElse);
            return List.copyOf(choices);
        }
    }

    public UpgradePath {
        steps = List.copyOf(steps);
        locksOut = List.copyOf(locksOut);
    }

    /** What every step costs together. */
    public int total() {
        return this.steps.stream().mapToInt(UpgradeNode::price).sum();
    }

    /** Whether nothing but money stands between the tower and the target. */
    public boolean isOpen() {
        return this.block.isEmpty() && !this.steps.isEmpty();
    }
}
