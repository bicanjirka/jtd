package td.ui.render;

/**
 * A bullet's streak: a slim line that runs from {@code tail} colour at its back end to {@code head} colour
 * at its front, the back end the faintest. {@code alpha} (0..1) fades the whole streak.
 */
public record TracerDraw(Palette tail, Palette head, float fromX, float fromY, float toX, float toY,
                         float strokeWidth, float alpha) implements TowerEffectDraw {
}
