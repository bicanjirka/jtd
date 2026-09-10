package td.ui.render;

/**
 * A dead enemy's death-fade animation, frozen at the position it died at (not
 * interpolated - it has stopped moving, so there is nothing to interpolate
 * toward). The corpse's outline expands as it fades out.
 *
 * @param growth       how far the outline has expanded past {@code scale}, in pixels. The
 *                     builder feeds ticks-since-death straight in, so the outline currently
 *                     grows one pixel per tick; the backend treats it purely as a distance,
 *                     so changing that rate needs no change here.
 * @param fadeProgress {@code 0} the tick it died, {@code 1} fully faded out
 */
public record EnemyFadeDraw(Palette palette, float x, float y, double facingRadians, float scale,
                             float growth, float fadeProgress) implements EnemyDraw {
}
