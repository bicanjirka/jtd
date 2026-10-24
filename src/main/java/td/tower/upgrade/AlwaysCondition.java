package td.tower.upgrade;

import td.term.TermText;
import td.tower.Tower;
import td.util.GameWorld;

/** The identity for {@link UpgradeCondition#and}: always satisfied. */
final class AlwaysCondition implements UpgradeCondition {

    static final UpgradeCondition INSTANCE = new AlwaysCondition();

    private AlwaysCondition() {
    }

    @Override
    public boolean isSatisfied(Tower tower, UpgradeState owned, GameWorld context) {
        return true;
    }

    @Override
    public TermText describe() {
        return TermText.of("money only");
    }

    @Override
    public UpgradeCondition and(UpgradeCondition other) {
        return other;
    }
}
