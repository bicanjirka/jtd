package td.ui.render;

/**
 * One {@code td.tower.upgrade.UpgradeSlot}'s mark on a tower's sprite: {@code level} filled
 * pips, one per node currently owned in that slot, and whether the slot is {@code ready} - it
 * currently offers a node whose gate is satisfied and which is affordable right now. A tower's
 * sprite always carries exactly three, one per slot, in slot (BASE/HEAD/SPECIAL) order - see
 * {@code TowerSpriteFrameBuilder.slotMarksFor}. Two different signals sharing one small draw
 * record: pips answer "what has this tower already bought", the chevron answers "what could it
 * buy right now".
 */
public record SlotMarkDraw(Palette palette, int level, boolean ready) {
}
