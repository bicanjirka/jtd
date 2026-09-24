package td.ui.render;

import java.util.List;

/**
 * A tower's body, plus its range ring when {@code selected}. {@code slotMarks} has one mark per
 * slot in order; {@code enchantPulse} is {@code 0} without a {@code SPECIAL} upgrade, otherwise a
 * 0..1 halo phase.
 */
public record TowerSpriteDraw(Palette palette, int boardX, int boardY, boolean selected,
                              float centerX, float centerY, float rangeReal,
                              List<SlotMarkDraw> slotMarks, float enchantPulse) {
}
