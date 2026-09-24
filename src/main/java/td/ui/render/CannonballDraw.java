package td.ui.render;

/** A shell in flight, position already interpolated. */
public record CannonballDraw(Palette palette, float x, float y) implements ProjectileDraw {
}
