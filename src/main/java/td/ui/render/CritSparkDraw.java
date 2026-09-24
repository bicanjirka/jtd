package td.ui.render;

/**
 * A brief burst where a critical hit landed.
 *
 * @param fadeProgress {@code 0} when it landed, {@code 1} fully faded
 */
public record CritSparkDraw(Palette palette, float x, float y, float scale, float fadeProgress) {
}
