package td.ui.render;

/**
 * What an effect sends off a live enemy, moving with it on the wall clock. It is both kinds of enemy draw: one comes
 * under the body, in the body list, and the rest over it.
 *
 * @param scale   the radius of what it plays around, in pixels
 * @param amount  how strongly it plays, from {@code 0} to {@code 1}; the backend adds what the size allows
 * @param seed    this enemy's, so its particles keep their timing for its life and differ from other enemies'
 * @param seconds the wall clock
 * @param alpha   {@code 1} in full view, fading with a cloaked body
 */
public record EnemyEmissionDraw(EnemyEmissionLook look, float x, float y, float scale, float amount, float seed,
                                float seconds, float alpha) implements EnemyDraw, EnemyOverlayDraw {
}
