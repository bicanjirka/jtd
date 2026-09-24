package td.ui.render;

/**
 * One travelling flame wave: a wedge from the origin along {@code headingRadians},
 * {@code halfWidthRadians} either side, out to {@code maxRadius}. The backend grows and fades it
 * from {@code progress} (0 to 1).
 */
public record ConeDraw(Palette palette, float originX, float originY, float headingRadians,
                       float maxRadius, float halfWidthRadians, float progress) implements TowerEffectDraw {
}
