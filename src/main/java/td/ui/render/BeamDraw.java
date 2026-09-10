package td.ui.render;

/**
 * A straight line between a tower and its target, or between a splash centre and a mob caught
 * in it. {@code strokeWidth} carries the shot's freshness - it is derived from the firing
 * tower's cooldown, so a beam thins out as the tower recharges.
 */
public record BeamDraw(Palette palette, float fromX, float fromY, float toX, float toY,
                        float strokeWidth) implements TowerEffectDraw {
}
