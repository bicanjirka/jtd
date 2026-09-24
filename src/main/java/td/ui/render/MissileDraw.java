package td.ui.render;

/**
 * A missile in flight, position already interpolated; {@code facingRadians} is its direction of
 * travel.
 */
public record MissileDraw(Palette palette, float x, float y, double facingRadians) implements ProjectileDraw {
}
