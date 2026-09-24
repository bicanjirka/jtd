package td.ui.render;

/**
 * A tower's animated turret head over its base. {@code headingRadians} rotates aiming and spinning
 * heads ({@code 0} along {@code +X}, toward {@code +Y}); {@code scale} sizes pulsing ones.
 */
public record TurretHeadDraw(Palette palette, float centerX, float centerY, float headingRadians, float scale) {
}
