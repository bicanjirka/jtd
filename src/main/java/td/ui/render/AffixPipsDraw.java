package td.ui.render;

import td.term.Term;

import java.util.List;

/**
 * One pip per affix over a live enemy, in the order its script wrote them, upright and beside its rank badge.
 *
 * @param x     the first pip's centre
 * @param y     every pip's centre
 * @param size  a pip's radius, the badge's size class
 * @param step  from one pip's centre to the next
 * @param alpha {@code 1} in full view, fading with a cloaked body
 */
public record AffixPipsDraw(float x, float y, float size, float step, float alpha, List<Term> affixes)
        implements EnemyOverlayDraw {
}
