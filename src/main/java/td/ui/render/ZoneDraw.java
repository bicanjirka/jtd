package td.ui.render;

/**
 * A patch of ground, drawn under everything that stands on it. {@code life} runs from 1 when it was
 * made to 0 when it ends, and {@code phase} is animation time, so a patch can flicker.
 */
public record ZoneDraw(Palette palette, float centerX, float centerY, float radius, float life, float phase) {
}
