package td.tower;

import td.tower.upgrade.UpgradeNode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The break between waves each purchase of a tower was made in, counted by the waves started before it. A purchase made
 * while a wave is out has none: it is used at once. What was bought in the break still running is unused.
 *
 * @param tower the break the tower was built in
 * @param nodes the break each owned node bought in a break was bought in, by id
 */
record PurchaseStamps(OptionalLong tower, Map<String, Long> nodes) {

    private static final TowerUse UNUSED_TOWER = new TowerUse(false, Set.of());

    PurchaseStamps {
        nodes = Map.copyOf(nodes);
    }

    static PurchaseStamps built(OptionalLong inBreak) {
        return new PurchaseStamps(inBreak, Map.of());
    }

    /** {@code use}, as of break {@code now}, for a tower rebuilt owning {@code owned}. */
    static PurchaseStamps of(TowerUse use, List<UpgradeNode> owned, long now) {
        Map<String, Long> nodes = owned.stream().filter(use::isUnused)
                .collect(Collectors.toMap(UpgradeNode::id, node -> now));
        return new PurchaseStamps(use.towerUsed() ? OptionalLong.empty() : OptionalLong.of(now), nodes);
    }

    /** {@code node} just bought, in break {@code inBreak} or while a wave was out. */
    PurchaseStamps withBought(UpgradeNode node, OptionalLong inBreak) {
        Map<String, Long> next = new HashMap<>(this.nodes);
        next.remove(node.id());
        inBreak.ifPresent(at -> next.put(node.id(), at));
        return new PurchaseStamps(this.tower, next);
    }

    /** {@code node} downgraded away. */
    PurchaseStamps withRemoved(UpgradeNode node) {
        Map<String, Long> next = new HashMap<>(this.nodes);
        next.remove(node.id());
        return new PurchaseStamps(this.tower, next);
    }

    /** What is unused once {@code started} waves have started: what was bought in the break after the last one. */
    TowerUse useAfter(long started) {
        if (this.tower.isPresent() && this.tower.getAsLong() == started) {
            return UNUSED_TOWER;
        }
        // Asked for every tower in every frame: builds nothing while nothing is unused.
        Set<String> unused = null;
        for (Map.Entry<String, Long> stamp : this.nodes.entrySet()) {
            if (stamp.getValue() == started) {
                unused = unused == null ? new HashSet<>() : unused;
                unused.add(stamp.getKey());
            }
        }
        return unused == null ? TowerUse.used() : new TowerUse(true, unused);
    }
}
