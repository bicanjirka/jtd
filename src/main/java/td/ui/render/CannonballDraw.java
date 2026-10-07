package td.ui.render;

/** A shell in flight, position already interpolated; {@code size} is a multiple of the standard shell. */
public record CannonballDraw(Palette palette, float x, float y, float size) implements ProjectileDraw {
}
