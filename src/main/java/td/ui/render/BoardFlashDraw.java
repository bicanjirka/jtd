package td.ui.render;

/**
 * A nuke's flash, which whitens the whole board for a moment: it went off at {@code x, y}, and {@code strength} runs
 * from 1 as it does to 0 as the board clears. While it is strong, every enemy shows only as its silhouette, its shadow
 * thrown away from the flash.
 */
public record BoardFlashDraw(float x, float y, float strength) {
}
