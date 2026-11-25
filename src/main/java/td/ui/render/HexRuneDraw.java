package td.ui.render;

/**
 * One hex's rune over the enemy carrying it, drawn {@code scale} pixels from its centre to its tips; {@code seconds} is
 * the wall clock its ring turns and its glyph breathes by.
 */
public record HexRuneDraw(HexGlyph glyph, float x, float y, float scale, float seconds) implements EnemyOverlayDraw {
}
