package td.ui.render;

/**
 * A wedge of a tower's flame cone, extending {@code radius} pixels from
 * ({@code originX}, {@code originY}) in {@code headingRadians}, {@code halfWidthRadians} either
 * side. The same heading {@code InWedgeTargetQuery} decides hits against (see
 * {@code td.tower.CinderTower}), interpolated the same way a turret head's own heading is.
 */
public record ConeDraw(Palette palette, float originX, float originY, float headingRadians,
                       float radius, float halfWidthRadians, float alpha) implements TowerEffectDraw {
}
