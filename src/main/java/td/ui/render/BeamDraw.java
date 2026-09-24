package td.ui.render;

/**
 * A line from a tower to its target, or from a splash centre to a mob it caught.
 * {@code strokeWidth} thins as the tower recharges.
 */
public record BeamDraw(Palette palette, float fromX, float fromY, float toX, float toY,
                       float strokeWidth) implements TowerEffectDraw {
}
