package td.ui.render;

/**
 * One travelling flame wave: a band from the origin along {@code headingRadians},
 * {@code halfWidthRadians} either side, whose front is {@code progress} (0 to 1) of the way out to
 * {@code maxRadius}.
 */
public record ConeDraw(Palette palette, float originX, float originY, float headingRadians,
                       float maxRadius, float halfWidthRadians, float progress) implements TowerEffectDraw {
}
