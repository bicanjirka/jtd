package td.ui.render;

/**
 * A rank badge over a live enemy, upright whatever the body's facing; a grunt has none.
 *
 * @param y     the badge's centre
 * @param size  the same on every enemy
 * @param alpha {@code 1} in full view, fading with a cloaked body
 */
public record RankBadgeDraw(RankBadge badge, float x, float y, float size, float alpha) implements EnemyOverlayDraw {
}
