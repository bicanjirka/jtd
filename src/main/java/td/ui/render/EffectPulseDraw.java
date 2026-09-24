package td.ui.render;

/**
 * A brief ring on a mob for a one-off moment: an effect gained or lost, a cast, or a spawn. The
 * backend grows or shrinks and fades it from {@code progress} (0 to 1).
 */
public record EffectPulseDraw(Palette palette, float centerX, float centerY, float radius, float progress,
                              PulseDirection direction) implements EnemyOverlayDraw {
}
