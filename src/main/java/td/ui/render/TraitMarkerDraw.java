package td.ui.render;

/**
 * A small hollow glyph naming one of a mob's always-on {@code Trait}s, drawn in a row below the
 * body - visually distinct from the filled dots of the timed status-effect row above it (see
 * {@link StatusMarkerDraw}), since a trait is permanent for this mob's lifetime rather than a
 * transient state. Not tied to any one enemy shape - see {@code td.ui.EnemyFrameBuilder}.
 */
public record TraitMarkerDraw(Palette palette, float x, float y, float scale) implements EnemyOverlayDraw {
}
