package td.ui.render;

/**
 * A dead enemy's fade, at the position it died. Its outline expands as it fades.
 *
 * @param growth       how far the outline has expanded past {@code scale}, in pixels
 * @param fadeProgress {@code 0} when it died, {@code 1} fully faded
 */
public record EnemyFadeDraw(Palette palette, float x, float y, double facingRadians, float scale,
                            float growth, float fadeProgress) implements EnemyDraw {
}
