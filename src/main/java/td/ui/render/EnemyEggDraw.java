package td.ui.render;

/**
 * The outline around a shielded enemy, whatever its shape. It adds no colour to what is inside and doesn't follow how
 * much shield is left: the bar says that.
 *
 * @param radiusX half its width, in pixels
 * @param radiusY half its height, in pixels
 * @param flash   {@code 1} when a hit just landed on a shield, falling to {@code 0}
 * @param seed    this enemy's, so eggs don't breathe in step
 * @param seconds the wall clock
 * @param alpha   {@code 1} in full view, fading with a cloaked body
 */
public record EnemyEggDraw(float x, float y, float radiusX, float radiusY, float flash, float seed, float seconds,
                           float alpha) implements EnemyOverlayDraw {
}
