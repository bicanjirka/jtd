package td.ui.render;

/**
 * The ice around a frozen enemy.
 *
 * @param scale   the radius of what it encases, in pixels
 * @param variant which block of ice this enemy gets, the same for its whole life
 * @param seed    this enemy's, so ice doesn't glint in step
 * @param seconds the wall clock
 * @param alpha   {@code 1} in full view, fading with a cloaked body
 */
public record EnemyIceDraw(float x, float y, float scale, int variant, float seed, float seconds,
                           float alpha) implements EnemyOverlayDraw {
}
