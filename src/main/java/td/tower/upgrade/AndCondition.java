package td.tower.upgrade;

import td.tower.Tower;
import td.util.GameWorld;

import java.util.HashSet;
import java.util.Set;

record AndCondition(UpgradeCondition left, UpgradeCondition right) implements UpgradeCondition {

    @Override
    public boolean isSatisfied(Tower tower, GameWorld context) {
        return this.left.isSatisfied(tower, context) && this.right.isSatisfied(tower, context);
    }

    @Override
    public Set<String> requiredNodes() {
        Set<String> both = new HashSet<>(this.left.requiredNodes());
        both.addAll(this.right.requiredNodes());
        return both;
    }

    @Override
    public String describe() {
        return this.left.describe() + " and " + this.right.describe();
    }

    @Override
    public String progress(Tower tower, GameWorld context) {
        return this.left.progress(tower, context) + ", " + this.right.progress(tower, context);
    }
}
