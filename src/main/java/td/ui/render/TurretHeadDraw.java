package td.ui.render;

/**
 * A tower's animated turret head, layered on top of its (static) {@link TowerSpriteDraw} base.
 * {@code headingRadians} carries an aiming/spinning tower's rotation (unused, always {@code 0},
 * for a pulsing tower); {@code scale} carries a pulsing tower's size (constant for every other
 * tower). {@code 0} radians points along {@code +X}, increasing toward {@code +Y} - the same
 * convention {@code Graphics2D.rotate(double)} itself uses.
 */
public record TurretHeadDraw(Palette palette, float centerX, float centerY, float headingRadians, float scale) {
}
