package td.ui.render;

/**
 * A colour laid over a live enemy's own body, matching it as it turns and swells.
 *
 * @param strength how strongly it shows, from {@code 0} to {@code 1}
 * @param alpha    {@code 1} in full view, fading with a cloaked body
 */
public record EnemyTintDraw(EnemyLook body, float x, float y, double facingRadians, float scale, EnemyTintLook tint,
                            float strength, float alpha) implements EnemyOverlayDraw {
}
