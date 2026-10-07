package td.ui.render;

/**
 * One travelling flame wave: a band from the origin along {@code headingRadians},
 * {@code halfWidthRadians} either side, whose front is {@code progress} (0 to 1) of the way out to
 * {@code maxRadius}. {@code stokeSteps} brighter, narrower cores lie inside the band.
 */
public record ConeDraw(Palette palette, float originX, float originY, float headingRadians,
                       float maxRadius, float halfWidthRadians, float progress,
                       int stokeSteps) implements TowerEffectDraw {

    public ConeDraw(Palette palette, float originX, float originY, float headingRadians, float maxRadius,
                    float halfWidthRadians, float progress) {
        this(palette, originX, originY, headingRadians, maxRadius, halfWidthRadians, progress, 0);
    }
}
