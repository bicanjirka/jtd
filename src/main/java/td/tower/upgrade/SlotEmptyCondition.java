package td.tower.upgrade;

import td.tower.Tower;
import td.util.GameWorld;

/** Satisfied while nothing is bought in {@code slot}. */
record SlotEmptyCondition(UpgradeSlot slot) implements UpgradeCondition {

    @Override
    public boolean isSatisfied(Tower tower, GameWorld context) {
        return tower.upgrades().countIn(this.slot) == 0;
    }

    @Override
    public String describe() {
        return "nothing chosen in " + this.slot;
    }
}
