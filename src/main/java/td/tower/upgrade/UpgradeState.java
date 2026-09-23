package td.tower.upgrade;

import td.tower.buff.TowerBuff;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A tower's owned upgrade nodes, in the order they were bought - the correlated set that
 * replaces {@code AbstractTower}'s old single {@code chosenPath} field. One immutable snapshot
 * published through a single {@code volatile} (root {@code CLAUDE.md} 3), not three independent
 * per-slot fields: a slot's own "current node" is {@link #tip(UpgradeSlot)}, the last-owned
 * node in that slot, so nothing here needs a second, separately-updated index to stay in sync.
 */
public record UpgradeState(List<UpgradeNode> owned) {

    private static final UpgradeState NONE = new UpgradeState(List.of());

    public UpgradeState {
        owned = List.copyOf(owned);
    }

    public static UpgradeState none() {
        return NONE;
    }

    /**
     * This state plus {@code node}, appended after everything already owned.
     */
    public UpgradeState with(UpgradeNode node) {
        List<UpgradeNode> next = new ArrayList<>(this.owned);
        next.add(node);
        return new UpgradeState(next);
    }

    public boolean owns(String nodeId) {
        return this.owned.stream().anyMatch(n -> n.id().equals(nodeId));
    }

    public List<UpgradeNode> inSlot(UpgradeSlot slot) {
        return this.owned.stream().filter(n -> n.slot() == slot).toList();
    }

    /**
     * The most recently bought node in {@code slot} - "at most one active node per slot", even
     * though a slot's own chain can hold several owned nodes (a level 1 and its level 2).
     */
    public Optional<UpgradeNode> tip(UpgradeSlot slot) {
        List<UpgradeNode> inSlot = this.inSlot(slot);
        return inSlot.isEmpty() ? Optional.empty() : Optional.of(inSlot.get(inSlot.size() - 1));
    }

    /**
     * Every owned node's {@code statBonus}, combined additively - folded into
     * {@code AbstractTower.recalculateStats()} alongside any external Aura buff, exactly the
     * way a single chosen path's bonus used to be.
     */
    public TowerBuff totalBuff() {
        return this.owned.stream().map(UpgradeNode::statBonus).reduce(TowerBuff.none(), TowerBuff::combine);
    }
}
