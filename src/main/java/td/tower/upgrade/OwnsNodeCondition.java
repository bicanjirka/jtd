package td.tower.upgrade;

import td.tower.Tower;
import td.util.GameWorld;

record OwnsNodeCondition(String nodeId) implements UpgradeCondition {

    @Override
    public boolean isSatisfied(Tower tower, GameWorld context) {
        return tower.upgrades().owns(this.nodeId);
    }

    @Override
    public String describe() {
        return "requires " + this.nodeId;
    }
}
