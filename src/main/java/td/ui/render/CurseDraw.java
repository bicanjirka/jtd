package td.ui.render;

/**
 * A hex flying to the enemy it curses: its rune as its head, {@code radius} pixels across, at {@code x, y} on screen,
 * with its tail behind it. {@code seconds} is the wall clock its rune turns by.
 */
public record CurseDraw(HexGlyph glyph, float x, float y, float radius, ShellTrail trail, float seconds)
        implements TowerEffectDraw {
}
