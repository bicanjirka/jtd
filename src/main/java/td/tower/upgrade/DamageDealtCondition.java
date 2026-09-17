package td.tower.upgrade;

import td.tower.Tower;
import td.util.GameWorld;

/**
 * Satisfied once this specific tower has dealt at least {@code threshold} total damage.
 */
public record DamageDealtCondition(long threshold) implements UpgradeCondition {

    @Override
    public boolean isSatisfied(Tower tower, GameWorld context) {
        return tower.getDamageDealt() >= this.threshold;
    }
}
