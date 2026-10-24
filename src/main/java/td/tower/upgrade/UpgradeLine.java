package td.tower.upgrade;

import java.util.Comparator;
import java.util.List;

/**
 * The nodes a tower owns of one line, in purchase order: a head chain with its level IV, the extra line, the Range
 * line, the Core chain, or one special.
 */
public record UpgradeLine(UpgradeNode first, List<UpgradeNode> owned) {

    public UpgradeLine {
        owned = List.copyOf(owned);
    }

    /** The owned node furthest along the line. */
    public UpgradeNode tip() {
        return this.owned.stream().max(Comparator.comparingInt(node -> node.tier().level())).orElseThrow();
    }

    /** 1 to 4; 0 for a special, which has no level. */
    public int level() {
        return this.tip().tier().level();
    }

    /** The node whose art the line shows: a Core step, level IV or special tip its own, every other line its first node's. */
    public UpgradeNode artNode() {
        return this.tip().tier().hasOwnArt() ? this.tip() : this.first;
    }
}
