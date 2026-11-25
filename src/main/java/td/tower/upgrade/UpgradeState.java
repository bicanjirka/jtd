package td.tower.upgrade;

import td.stat.StatModifiers;
import td.tower.TowerCapability;
import td.tower.TowerStat;
import td.tower.proc.HitBonus;
import td.tower.proc.Proc;
import td.tower.targeting.TargetPriority;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** A tower's owned nodes, a paid tier's unlock among them, in purchase order, as one immutable snapshot. */
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

    // Indexed loops: these run for every tower on every frame build, which has an allocation budget.
    public boolean owns(String nodeId) {
        for (int i = 0; i < this.owned.size(); i++) {
            if (this.owned.get(i).id().equals(nodeId)) {
                return true;
            }
        }
        return false;
    }

    /** How many owned nodes sit in {@code tier}, its unlock included. */
    public int countInTier(int tier) {
        int count = 0;
        for (int i = 0; i < this.owned.size(); i++) {
            if (this.owned.get(i).tier() == tier) {
                count++;
            }
        }
        return count;
    }

    /** What every owned node lets the tower do. */
    public Set<TowerCapability> capabilities() {
        Set<TowerCapability> all = EnumSet.noneOf(TowerCapability.class);
        for (UpgradeNode node : this.owned) {
            all.addAll(node.effects().capabilities());
        }
        return all;
    }

    /** Whom the first owned node that picks a target makes the tower aim at. */
    public Optional<TargetPriority> targeting() {
        for (UpgradeNode node : this.owned) {
            if (node.effects().targeting().isPresent()) {
                return node.effects().targeting();
            }
        }
        return Optional.empty();
    }

    /** Every owned node's hit bonuses, in purchase order. */
    public List<HitBonus> hitBonuses() {
        List<HitBonus> all = new ArrayList<>();
        for (UpgradeNode node : this.owned) {
            all.addAll(node.effects().hitBonuses());
        }
        return all;
    }

    /** Every owned node's procs, in purchase order. */
    public List<Proc> procs() {
        List<Proc> all = new ArrayList<>();
        for (UpgradeNode node : this.owned) {
            all.addAll(node.effects().procs());
        }
        return all;
    }

    /** Every owned node's emission, combined. */
    public StatModifiers<TowerStat> totalEmission() {
        return this.owned.stream().map(n -> n.effects().emission()).reduce(StatModifiers.none(), StatModifiers::plus);
    }
}
