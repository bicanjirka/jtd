package td.tower.upgrade;

import td.tower.Tower;
import td.util.GameWorld;

/**
 * Satisfied while nothing has been bought yet in {@code slot} - what makes two root nodes of
 * the same slot mutually exclusive once either one is chosen: the sibling root's own
 * {@code requires} stops being satisfied the moment the first node in that slot is owned.
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
