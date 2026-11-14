package td.ui.render;

import java.util.List;

/**
 * A tower's body, plus its range ring when {@code selected}. {@code slotMarks} has one mark per
 * tier, bottom first; {@code enchantPulse} is {@code 0} without an upgrade in its top tier, otherwise a
 * 0..1 halo phase. A {@code crowned} tower, its top tier open, wears a slow ring turned {@code haloTurn} (0..1)
 * of the way round, and its last top-tier pip is gold. {@code rank} pips (0 for a Recruit) show its
 * XP tier; {@code rankUpProgress} runs 0..1 through the glow after a rank-up, {@code -1} outside it.
 */
public record TowerSpriteDraw(TowerLook look, int boardX, int boardY, boolean selected,
                              float centerX, float centerY, float rangeReal,
                              List<SlotMarkDraw> slotMarks, float enchantPulse,
                              boolean crowned, float haloTurn, int rank, float rankUpProgress) {
}
