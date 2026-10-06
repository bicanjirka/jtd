package td.ui.render;

import java.util.List;

/**
 * A tower's body, plus its range ring when {@code selected}. {@code slotMarks} has one mark per
 * slot in order; {@code enchantPulse} is {@code 0} without a {@code SPECIAL} upgrade, otherwise a
 * 0..1 halo phase. A {@code transcendent} tower wears a slow ring turned {@code haloTurn} (0..1)
 * of the way round, and its last base pip is gold.
 */
public record TowerSpriteDraw(Palette palette, int boardX, int boardY, boolean selected,
                              float centerX, float centerY, float rangeReal,
                              List<SlotMarkDraw> slotMarks, float enchantPulse,
                              boolean transcendent, float haloTurn) {
}
