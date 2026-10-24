package td.tower.upgrade;

import td.term.TermText;
import td.tower.Tower;
import td.util.GameWorld;

/**
 * A purpose gate: the tower has done its job {@code threshold} times since Attune, counted as its
 * deeds (at most once per attack or per second, never per enemy). {@code deed} names one, e.g.
 * {@code "shots with [Steady Aim]"}.
 */
public record PurposeCondition(TermText deed, int threshold) implements UpgradeCondition {

    @Override
    public boolean isSatisfied(Tower tower, UpgradeState owned, GameWorld context) {
        return tower.experience().deeds() >= this.threshold;
    }

    @Override
    public TermText describe() {
        return TermText.of(this.threshold + " ").plus(this.deed);
    }

    @Override
    public float progressShare(Tower tower, GameWorld context) {
        return Math.min(1f, tower.experience().deeds() / (float) this.threshold);
    }

    /** {@code "shots with [Steady Aim] 12/20"}. */
    @Override
    public TermText progress(Tower tower, GameWorld context) {
        return this.deed.plus(" " + Math.min(tower.experience().deeds(), this.threshold) + "/" + this.threshold);
    }
}
