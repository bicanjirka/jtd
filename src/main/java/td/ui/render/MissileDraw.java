package td.ui.render;

/**
 * A missile in flight, position already interpolated; {@code facingRadians} is its direction of travel and
 * {@code length} how long it is drawn, in pixels.
 */
public record MissileDraw(LoadedMissile kind, float x, float y, double facingRadians, float length)
        implements ProjectileDraw {
}
