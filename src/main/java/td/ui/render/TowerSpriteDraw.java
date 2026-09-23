package td.ui.render;

import java.util.List;

/**
 * A tower's body sprite, plus its selection range ring when {@code selected}. {@code
 * slotMarks} carries one {@link SlotMarkDraw} per {@code td.tower.upgrade.UpgradeSlot}, always
 * three, in slot order; {@code enchantPulse} is {@code 0} for a tower with nothing owned in its
 * {@code SPECIAL} slot, otherwise a 0..1 phase driving a pulsing halo - the same "function of
 * elapsed time" cosmetic animation the Aura tower's own pulse already uses.
 */
public record TowerSpriteDraw(Palette palette, int boardX, int boardY, boolean selected,
                              float centerX, float centerY, float rangeReal,
                              List<SlotMarkDraw> slotMarks, float enchantPulse) {
}
