package td.tower.upgrade;

import td.term.TermText;
import td.tower.Tower;
import td.util.GameWorld;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

record AndCondition(UpgradeCondition left, UpgradeCondition right) implements UpgradeCondition {

    @Override
    public boolean isSatisfied(Tower tower, UpgradeState owned, GameWorld context) {
        return this.left.isSatisfied(tower, owned, context) && this.right.isSatisfied(tower, owned, context);
    }

    @Override
    public Set<String> requiredNodes() {
        Set<String> both = new HashSet<>(this.left.requiredNodes());
        both.addAll(this.right.requiredNodes());
        return both;
    }

    @Override
    public Set<String> missingNodes(Tower tower, UpgradeState owned, GameWorld context) {
        Set<String> both = new HashSet<>(this.left.missingNodes(tower, owned, context));
        both.addAll(this.right.missingNodes(tower, owned, context));
        return both;
    }

    @Override
    public boolean lockedOut(Predicate<String> nodeLockedOut) {
        return this.left.lockedOut(nodeLockedOut) || this.right.lockedOut(nodeLockedOut);
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
