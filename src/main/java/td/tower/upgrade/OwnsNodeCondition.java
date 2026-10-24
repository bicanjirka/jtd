package td.tower.upgrade;

import td.term.TermText;
import td.tower.Tower;
import td.util.GameWorld;

import java.util.Set;
import java.util.function.Predicate;

record OwnsNodeCondition(String nodeId) implements UpgradeCondition {

    @Override
    public boolean isSatisfied(Tower tower, UpgradeState owned, GameWorld context) {
        return owned.owns(this.nodeId);
    }

    @Override
    public Set<String> missingNodes(Tower tower, UpgradeState owned, GameWorld context) {
        return owned.owns(this.nodeId) ? Set.of() : Set.of(this.nodeId);
    }

    @Override
    public boolean lockedOut(Predicate<String> nodeLockedOut) {
        return nodeLockedOut.test(this.nodeId);
    }

    @Override
    public Set<String> requiredNodes() {
        return Set.of(this.nodeId);
    }

    @Override
    public TermText describe() {
        return TermText.of("requires " + this.nodeId);
    }
}
