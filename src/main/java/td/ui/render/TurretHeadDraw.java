package td.ui.render;

/**
 * A tower's animated turret head over its base. {@code headingRadians} rotates aiming and spinning
 * heads ({@code 0} along {@code +X}, toward {@code +Y}); {@code scale} sizes pulsing ones. A
 * {@code gilded} head is drawn gold instead of in its look's colour.
 */
public record TurretHeadDraw(TowerLook look, boolean gilded, float centerX, float centerY, float headingRadians, float scale) {
}
