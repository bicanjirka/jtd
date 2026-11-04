package td.ui.render;

/**
 * A short burst around a live enemy, following game time since it reports an event.
 *
 * @param radiusX  how far the burst starts from the centre across, in pixels
 * @param radiusY  how far it starts from the centre up and down, in pixels
 * @param progress {@code 0} when it happened, {@code 1} when gone
 * @param seed     this enemy's, so bursts differ
 * @param alpha    {@code 1} in full view, fading with a cloaked body
 */
public record EnemyBurstDraw(EnemyBurstLook look, float x, float y, float radiusX, float radiusY, float progress,
                             float seed, float alpha) implements EnemyOverlayDraw {
}
