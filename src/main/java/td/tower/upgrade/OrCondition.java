package td.tower.upgrade;

import td.tower.Tower;
import td.util.GameWorld;

import java.util.Set;

/** Lets a node be reached from more than one earlier choice. */
record OrCondition(UpgradeCondition left, UpgradeCondition right) implements UpgradeCondition {

    @Override
    public boolean isSatisfied(Tower tower, GameWorld context) {
        return this.left.isSatisfied(tower, context) || this.right.isSatisfied(tower, context);
    }

    @Override
    public Set<String> requiredNodes() {
        Set<String> left = this.left.requiredNodes();
        Set<String> right = this.right.requiredNodes();
        return left.size() <= right.size() ? left : right;
    }

    @Override
    public String describe() {
        return this.left.describe() + " or " + this.right.describe();
    }

    @Override
    public String progress(Tower tower, GameWorld context) {
        return this.left.progress(tower, context) + " or " + this.right.progress(tower, context);
    }
}
