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

    @Override
    public String describe() {
        // threshold is in hundredths, the same scale getDamageDealt() reports in - see
        // AbstractTower.getStatusString()'s own "Damage dealt" line for the precedent.
        return (this.threshold / 100f) + " damage dealt";
    }
}
