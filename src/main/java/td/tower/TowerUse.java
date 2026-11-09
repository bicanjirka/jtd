package td.tower;

import td.tower.upgrade.UpgradeNode;

import java.util.HashSet;
import java.util.Set;

/**
 * What of a tower has been used: owned while a wave ran. What is unused (the tower bought since the last wave, or a
 * node bought since) sells back at its full price.
 *
 * @param towerUsed   whether the tower itself has been through a wave
 * @param unusedNodes the ids of the owned nodes bought since the last wave ran; all of them, for an unused tower
 */
public record TowerUse(boolean towerUsed, Set<String> unusedNodes) {

    private static final TowerUse UNUSED = new TowerUse(false, Set.of());
    private static final TowerUse USED = new TowerUse(true, Set.of());

    public TowerUse {
        unusedNodes = Set.copyOf(unusedNodes);
    }

    /** A tower just built, before any wave ran with it. */
    public static TowerUse unused() {
        return UNUSED;
    }

    /** A tower and everything it owns through a wave. */
    public static TowerUse used() {
        return USED;
    }

    /** {@code node} just bought. */
    public TowerUse withBought(UpgradeNode node) {
        Set<String> more = new HashSet<>(this.unusedNodes);
        more.add(node.id());
        return new TowerUse(this.towerUsed, more);
    }

    /** {@code node} downgraded away, so buying it again buys it unused. */
    public TowerUse withRemoved(UpgradeNode node) {
        Set<String> fewer = new HashSet<>(this.unusedNodes);
        fewer.remove(node.id());
        return new TowerUse(this.towerUsed, fewer);
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
