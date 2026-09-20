package td.ui.render;

/**
 * A brief ring marking a one-shot moment on a mob - an effect kind's gain or loss, an ability's
 * cast, or an ability-driven spawn's arrival - centred on the mob it happened to. Shaped like
 * {@link CritSparkDraw}: position, a target {@code radius}, and a 0..1 {@code progress} the
 * backend turns into growth-or-shrink plus a fade, rather than a stored current radius/alpha -
 * see {@code td.ui.EnemyFrameBuilder}.
 */
public record EffectPulseDraw(Palette palette, float centerX, float centerY, float radius, float progress,
                              PulseDirection direction) implements EnemyOverlayDraw {
}
