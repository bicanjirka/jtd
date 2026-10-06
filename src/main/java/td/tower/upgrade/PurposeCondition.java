package td.tower.upgrade;

import td.tower.Tower;
import td.util.GameWorld;

/**
 * A purpose gate: the tower has done its job {@code threshold} times since Attune, counted as its
 * deeds (at most once per attack or per second, never per enemy). {@code deed} names one, e.g.
 * {@code "Steady Aim shots"}.
 */
public record PurposeCondition(String deed, int threshold) implements UpgradeCondition {

    @Override
    public boolean isSatisfied(Tower tower, GameWorld context) {
        return tower.experience().deeds() >= this.threshold;
    }

    @Override
    public String describe() {
        return this.threshold + " " + this.deed;
    }

    /** {@code "Steady Aim shots 12/20"}. */
    @Override
    public String progress(Tower tower, GameWorld context) {
        return this.deed + " " + Math.min(tower.experience().deeds(), this.threshold) + "/" + this.threshold;
    }
}
