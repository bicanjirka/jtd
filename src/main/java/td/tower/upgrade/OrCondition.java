package td.tower.upgrade;

import td.tower.Tower;
import td.util.GameWorld;

/**
 * Either {@code left} or {@code right} being satisfied is enough - built through
 * {@link UpgradeCondition#or}, mirroring {@code TargetQuery.and}'s combinator shape. Lets a
 * slot's graph reconverge: a node reachable from more than one earlier choice.
 */
record OrCondition(UpgradeCondition left, UpgradeCondition right) implements UpgradeCondition {

    @Override
    public boolean isSatisfied(Tower tower, GameWorld context) {
        return this.left.isSatisfied(tower, context) || this.right.isSatisfied(tower, context);
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
