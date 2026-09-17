package td.tower.upgrade;

import td.tower.Tower;
import td.util.GameWorld;

/**
 * The identity element for {@link UpgradeCondition} - satisfied unconditionally.
 */
final class AlwaysCondition implements UpgradeCondition {

    static final UpgradeCondition INSTANCE = new AlwaysCondition();

    private AlwaysCondition() {
    }

    @Override
    public boolean isSatisfied(Tower tower, GameWorld context) {
        return true;
    }
}
