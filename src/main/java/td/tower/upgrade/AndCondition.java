package td.tower.upgrade;

import td.tower.Tower;
import td.util.GameWorld;

/**
 * Both {@code left} and {@code right} must be satisfied - built through
 * {@link UpgradeCondition#and}, mirroring {@code TargetQuery.and}'s combinator shape.
 */
record AndCondition(UpgradeCondition left, UpgradeCondition right) implements UpgradeCondition {

    @Override
    public boolean isSatisfied(Tower tower, GameWorld context) {
        return this.left.isSatisfied(tower, context) && this.right.isSatisfied(tower, context);
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
