package td.tower;

import td.tower.upgrade.UpgradeNode;

import java.util.Set;

/**
 * What of a tower has been used: owned while a wave ran. What is unused (the tower built since the last wave, or a node
 * bought since) sells back at its full price.
 *
 * @param towerUsed   whether the tower itself has been through a wave
 * @param unusedNodes of a used tower, the ids of the owned nodes bought since the last wave ran; empty for an unused
 *                    tower, all of whose nodes are unused
 */
public record TowerUse(boolean towerUsed, Set<String> unusedNodes) {

    private static final TowerUse USED = new TowerUse(true, Set.of());

    public TowerUse {
        unusedNodes = towerUsed ? Set.copyOf(unusedNodes) : Set.of();
    }

    /** A tower and everything it owns through a wave. */
    public static TowerUse used() {
        return USED;
    }

    public boolean isUnused(UpgradeNode node) {
        return !this.towerUsed || this.unusedNodes.contains(node.id());
    }

    /** Whether any of it would sell back in full. */
    public boolean anyUnused() {
        return !this.towerUsed || !this.unusedNodes.isEmpty();
    }

    /** Whether all of it would: the tower has not been through a wave. */
    public boolean allUnused() {
        return !this.towerUsed;
    }
}
