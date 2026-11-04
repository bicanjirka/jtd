package td.ui.render;

import java.util.List;

/**
 * A row of small dots under a live enemy, for effects with no picture of their own.
 *
 * @param x     the centre of the row
 * @param y     the centre of every dot
 * @param alpha {@code 1} in full view, fading with a cloaked body
 */
public record EnemyDotsDraw(float x, float y, float alpha, List<EnemyDotLook> dots) implements EnemyOverlayDraw {
}
