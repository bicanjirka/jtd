package td.tower.upgrade;

import td.tower.buff.TowerBuff;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A tower's owned nodes in purchase order, as one immutable snapshot. A slot's current node is
 * derived by {@link #tip(UpgradeSlot)}, so there is no separate index to keep in sync.
 */
public record UpgradeState(List<UpgradeNode> owned) {

    private static final UpgradeState NONE = new UpgradeState(List.of());

    public UpgradeState {
        owned = List.copyOf(owned);
    }

    public static UpgradeState none() {
        return NONE;
    }

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

    /** The last node bought in {@code slot}. */
    public Optional<UpgradeNode> tip(UpgradeSlot slot) {
        List<UpgradeNode> inSlot = this.inSlot(slot);
        return inSlot.isEmpty() ? Optional.empty() : Optional.of(inSlot.get(inSlot.size() - 1));
    }

    /** Every owned node's bonus, combined. */
    public TowerBuff totalBuff() {
        return this.owned.stream().map(UpgradeNode::statBonus).reduce(TowerBuff.none(), TowerBuff::combine);
    }
}
