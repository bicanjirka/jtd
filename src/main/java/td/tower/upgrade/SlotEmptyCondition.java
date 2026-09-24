package td.tower.upgrade;

import td.tower.Tower;
import td.util.GameWorld;

/**
 * Satisfied while nothing is bought in {@code slot}, which makes a slot's root nodes mutually
 * exclusive.
 */
record SlotEmptyCondition(UpgradeSlot slot) implements UpgradeCondition {

    @Override
    public boolean isSatisfied(Tower tower, GameWorld context) {
        return tower.upgrades().tip(this.slot).isEmpty();
    }

    @Override
    public String describe() {
        return "nothing chosen in " + this.slot;
    }
}
