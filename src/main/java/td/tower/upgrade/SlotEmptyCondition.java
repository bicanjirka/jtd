package td.tower.upgrade;

import td.term.TermText;
import td.tower.Tower;
import td.util.GameWorld;

/** Satisfied while nothing is bought in {@code slot}. */
record SlotEmptyCondition(UpgradeSlot slot) implements UpgradeCondition {

    @Override
    public boolean isSatisfied(Tower tower, UpgradeState owned, GameWorld context) {
        return owned.countIn(this.slot) == 0;
    }

    @Override
    public TermText describe() {
        return TermText.of("nothing chosen in " + this.slot);
    }
}
