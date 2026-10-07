package td.tower.upgrade;

import td.tower.Tower;
import td.util.GameWorld;

import java.util.Set;

record OwnsNodeCondition(String nodeId) implements UpgradeCondition {

    @Override
    public boolean isSatisfied(Tower tower, GameWorld context) {
        return tower.upgrades().owns(this.nodeId);
    }

    @Override
    public Set<String> requiredNodes() {
        return Set.of(this.nodeId);
    }

    @Override
    public String describe() {
        return "requires " + this.nodeId;
    }
}
