package td.ui.render;

/** One hex's rune over the enemy carrying it, drawn {@code scale} pixels from its centre to its tips. */
public record HexRuneDraw(HexGlyph glyph, float x, float y, float scale) implements EnemyOverlayDraw {
}
