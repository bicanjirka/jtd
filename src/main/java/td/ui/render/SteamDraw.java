package td.ui.render;

/**
 * Steam let off at a nozzle's tip ({@code x}, {@code y}, already lifted), {@code progress} 0..1 through it;
 * {@code seed} keeps one tower's puffs its own.
 */
public record SteamDraw(float x, float y, float cellPixels, float progress, int seed) implements TowerEffectDraw {
}
