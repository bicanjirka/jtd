package td.ui.render;

/**
 * A missile in flight, position already interpolated; {@code facingRadians} is its direction of
 * travel and {@code size} a multiple of the standard missile's.
 */
public record MissileDraw(Palette palette, float x, float y, double facingRadians, float size)
        implements ProjectileDraw {
}
