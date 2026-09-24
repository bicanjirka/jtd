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
        // threshold is in hundredths, like getDamageDealt()
        return (this.threshold / 100f) + " damage dealt";
    }

    @Override
    public String progress(Tower tower, GameWorld context) {
        long current = Math.min(tower.getDamageDealt(), this.threshold);
        return (current / 100f) + "/" + (this.threshold / 100f) + " damage dealt";
    }
}
