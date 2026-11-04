package td.ui.render;

/**
 * One zone's disc in its patch. {@code radius} is as drawn, already shrunk by an ending zone. {@code fade} runs from 1
 * while it has time left to 0 when it ends, and {@code phase} is animation time, so what stands on the disc can flicker.
 */
public record ZoneDraw(float centerX, float centerY, float radius, float fade, float phase) {
}
