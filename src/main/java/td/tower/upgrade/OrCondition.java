package td.tower.upgrade;

import td.term.TermText;
import td.tower.Tower;
import td.util.GameWorld;

import java.util.Set;
import java.util.function.Predicate;

/** Lets a node be reached from more than one earlier choice. */
record OrCondition(UpgradeCondition left, UpgradeCondition right) implements UpgradeCondition {

    @Override
    public boolean isSatisfied(Tower tower, UpgradeState owned, GameWorld context) {
        return this.left.isSatisfied(tower, owned, context) || this.right.isSatisfied(tower, owned, context);
    }

    @Override
    public Set<String> requiredNodes() {
        Set<String> left = this.left.requiredNodes();
        Set<String> right = this.right.requiredNodes();
        return left.size() <= right.size() ? left : right;
    }

    /** A side that lacks no node yet isn't met is one no purchase meets, so the other side is taken. */
    @Override
    public Set<String> missingNodes(Tower tower, UpgradeState owned, GameWorld context) {
        if (this.isSatisfied(tower, owned, context)) {
            return Set.of();
        }
        Set<String> left = this.left.missingNodes(tower, owned, context);
        Set<String> right = this.right.missingNodes(tower, owned, context);
        if (left.isEmpty() || right.isEmpty()) {
            return left.isEmpty() ? right : left;
        }
        return left.size() <= right.size() ? left : right;
    }

    @Override
    public boolean lockedOut(Predicate<String> nodeLockedOut) {
        return this.left.lockedOut(nodeLockedOut) && this.right.lockedOut(nodeLockedOut);
    }

    @Override
    public TermText describe() {
        return this.left.describe().plus(" or ").plus(this.right.describe());
    }

    @Override
    public TermText progress(Tower tower, GameWorld context) {
        return this.left.progress(tower, context).plus(" or ").plus(this.right.progress(tower, context));
    }
}
