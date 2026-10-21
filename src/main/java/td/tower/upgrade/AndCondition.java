package td.tower.upgrade;

import td.term.TermText;
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
    public TermText describe() {
        return this.left.describe().plus(" and ").plus(this.right.describe());
    }

    @Override
    public TermText progress(Tower tower, GameWorld context) {
        return this.left.progress(tower, context).plus(", ").plus(this.right.progress(tower, context));
    }
}
