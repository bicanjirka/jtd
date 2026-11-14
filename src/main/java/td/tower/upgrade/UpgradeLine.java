package td.tower.upgrade;

import java.util.List;

/**
 * The upgrades a tower owns of one line, in purchase order: a node and what it comes after, back to the line's first.
 *
 * @param level   how far along the line its furthest owned node is, 1 for the first alone
 * @param artNode the node whose art the line shows
 */
public record UpgradeLine(UpgradeNode first, List<UpgradeNode> owned, int level, UpgradeNode artNode) {

    public UpgradeLine {
        owned = List.copyOf(owned);
    }

    /** The owned node furthest along the line. */
    public UpgradeNode tip() {
        return this.owned.getLast();
    }
}
