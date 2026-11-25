package td.ui.render;

/**
 * A Thunderstrike: one great bolt from the board's top edge down onto the ground at {@code x, y}, where its enemy
 * stood. {@code progress} runs 0..1 over the strike, {@code scale} is a cell in pixels, {@code seed} shapes the bolt.
 */
public record ThunderboltDraw(float x, float y, float progress, int scale, int seed) implements TowerEffectDraw {
}
