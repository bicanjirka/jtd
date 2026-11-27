package td.ui.render;

/**
 * Piece {@code index} of {@code count} standing on one disc of a payload ground (a flame, a shard of ice), painted among
 * the enemies by where it stands, so what stands in the fire stands among its flames.
 */
public record ZoneGroundPieceDraw(Palette palette, ZoneDraw zone, int index, int count) implements BoardDraw {
}
