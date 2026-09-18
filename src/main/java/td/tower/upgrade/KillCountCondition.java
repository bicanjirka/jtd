package td.tower.upgrade;

import td.tower.Tower;
import td.util.GameWorld;

/**
 * Satisfied once this specific tower has killed at least {@code threshold} enemies.
 */
public record KillCountCondition(int threshold) implements UpgradeCondition {

    @Override
    public boolean isSatisfied(Tower tower, GameWorld context) {
        return tower.getKillCount() >= this.threshold;
    }

    @Override
    public String describe() {
        return this.threshold + " kills";
    }
}
