package td.ui.render;

import java.util.List;

/**
 * A tower's body, plus its range ring when {@code selected}. {@code slotMarks} has one mark per
 * slot in order; {@code enchantPulse} is {@code 0} without a {@code SPECIAL} upgrade, otherwise a
 * 0..1 halo phase. A {@code transcendent} tower wears a slow ring turned {@code haloTurn} (0..1)
 * of the way round, and its last base pip is gold. {@code rank} pips (0 for a Recruit) show its
 * XP tier; {@code rankUpProgress} runs 0..1 through the glow after a rank-up, {@code -1} outside it.
 * A tower that {@code aims} also gives the heading its body may turn to face.
 */
public record TowerSpriteDraw(Palette palette, int boardX, int boardY, boolean selected,
                              float centerX, float centerY, float rangeReal,
                              List<SlotMarkDraw> slotMarks, float enchantPulse,
                              boolean transcendent, float haloTurn, int rank, float rankUpProgress,
                              boolean aims, float facingRadians) {

    /** A tower that does not aim. */
    public TowerSpriteDraw(Palette palette, int boardX, int boardY, boolean selected, float centerX, float centerY,
                           float rangeReal, List<SlotMarkDraw> slotMarks, float enchantPulse, boolean transcendent,
                           float haloTurn, int rank, float rankUpProgress) {
        this(palette, boardX, boardY, selected, centerX, centerY, rangeReal, slotMarks, enchantPulse, transcendent,
                haloTurn, rank, rankUpProgress, false, 0f);
    }
}
