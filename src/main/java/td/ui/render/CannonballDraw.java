package td.ui.render;

/**
 * A lobbed shell in flight, position already interpolated: {@code x, y} is its point on the ground, where its
 * shadow lies, and it is drawn {@code lift} pixels above it. The shadow shrinks to {@code shadowScale} of its
 * size as the shell climbs. {@code size} is a multiple of the standard shell. A shell with a streak trails a
 * line to where it was drawn a tick ago; without one the tail is the head.
 */
public record CannonballDraw(Palette palette, float x, float y, float lift, float shadowScale, float size,
                             float tailX, float tailY) implements ProjectileDraw, TowerEffectDraw {
}
