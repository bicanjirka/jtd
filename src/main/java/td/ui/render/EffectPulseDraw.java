package td.ui.render;

/**
 * A brief ring on a mob for a one-off moment: a cast or a spawn. The backend grows and fades it from
 * {@code progress} (0 to 1).
 */
public record EffectPulseDraw(Palette palette, float centerX, float centerY, float radius, float progress) implements EnemyOverlayDraw {
}
