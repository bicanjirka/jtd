package td.ui.render;

/**
 * A shell in flight, position already interpolated; {@code size} is a multiple of the standard shell.
 * A shell with a streak trails a line to where it was a tick ago; without one the tail is the head.
 */
public record CannonballDraw(Palette palette, float x, float y, float size, float tailX,
                             float tailY) implements ProjectileDraw {
}
