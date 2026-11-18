package td.ui.render;

/**
 * A mortar's spit at its mouth ({@code x}, {@code y}, already lifted to the mouth's top), {@code progress} 0..1 through
 * it: a flash on the shot's first tick, smoke puffing out sideways round the mouth, and a smoke ring rising off it.
 */
public record MuzzleBlastDraw(float x, float y, float cellPixels, float progress) implements TowerEffectDraw {
}
