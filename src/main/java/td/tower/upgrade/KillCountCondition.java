package td.tower.upgrade;

import td.tower.Tower;
import td.util.GameWorld;

/** Satisfied once this tower has at least {@code threshold} kills. */
public record KillCountCondition(int threshold) implements UpgradeCondition {

    @Override
    public boolean isSatisfied(Tower tower, GameWorld context) {
        return tower.getKillCount() >= this.threshold;
    }

    @Override
    public String describe() {
        return this.threshold + " kills";
    }

    @Override
    public String progress(Tower tower, GameWorld context) {
        return Math.min(tower.getKillCount(), this.threshold) + "/" + this.threshold + " kills";
    }
}
