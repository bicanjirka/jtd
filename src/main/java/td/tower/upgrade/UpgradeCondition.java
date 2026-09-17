package td.tower.upgrade;

import td.tower.Tower;
import td.util.GameWorld;

/**
 * Whether an upgrade path is currently available to a specific tower - the gate a player
 * must clear before spending on it, evaluated fresh every time it's asked rather than
 * cached. This is independent of affordability: a caller checks both
 * {@code isSatisfied(...)} and the path's price separately, the same way the toolbar
 * already separates "is this tower type unlocked" from "can I afford it".
 * <p>
 * {@link #always()} is the identity - satisfied unconditionally - which is what a path
 * gated on money alone uses: affordability is the only real gate it has.
 */
public interface UpgradeCondition {

    static UpgradeCondition always() {
        return AlwaysCondition.INSTANCE;
    }

    boolean isSatisfied(Tower tower, GameWorld context);
}
