package td.ui.render;

/**
 * The dust a Strider of {@code tier} at ({@code centerX}, {@code centerY}) kicks up at every foot as it slams down,
 * {@code progress} 0..1 through it.
 */
public record FootDustDraw(int tier, float centerX, float centerY, float cellPixels, float progress)
        implements TowerEffectDraw {
}
