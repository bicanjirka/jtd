package td.ui.render;

/**
 * A small "miss" rising where a dodged hit would have landed, plain and grey so it never reads as a crit.
 *
 * @param x            just right of the enemy's body, where the text starts, in pixels
 * @param y            the enemy's centre, in pixels
 * @param fadeProgress {@code 0} when it dodged, {@code 1} fully faded
 */
public record MissMarkDraw(float x, float y, float fadeProgress) implements EnemyOverlayDraw {
}
