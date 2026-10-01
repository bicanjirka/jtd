package td.tower.upgrade;

import td.damage.DamageUnits;
import td.tower.Tower;
import td.util.GameWorld;

/** Satisfied once this tower has dealt at least {@code points} damage. */
public record DamageDealtCondition(long points) implements UpgradeCondition {

    @Override
    public boolean isSatisfied(Tower tower, GameWorld context) {
        return tower.getDamageDealt() >= this.threshold();
    }

    @Override
    public String describe() {
        return this.points + " damage dealt";
    }

    @Override
    public String progress(Tower tower, GameWorld context) {
        long current = Math.min(tower.getDamageDealt(), this.threshold());
        return current / DamageUnits.PER_POINT + "/" + this.points + " damage";
    }

    private long threshold() {
        return this.points * DamageUnits.PER_POINT;
    }
}
