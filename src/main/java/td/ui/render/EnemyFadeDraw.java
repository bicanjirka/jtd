package td.ui.render;

/**
 * A dead enemy's death-fade animation, frozen at the position it died at (not
 * interpolated - it has stopped moving, so there is nothing to interpolate
 * toward). {@code growth} is the outline's outward growth in pixels since death;
 * {@code fadeProgress} is {@code 0} (just died) to {@code 1} (fully faded).
 */
public record EnemyFadeDraw(Palette palette, float x, float y, double facingRadians, float scale,
                             float growth, float fadeProgress) implements EnemyDraw {
}
