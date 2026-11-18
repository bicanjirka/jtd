package td.ui.render;

/**
 * A puff of steam let off at the foot of a mortar's pump ({@code x}, {@code y}, already lifted), {@code progress} 0..1
 * through it: as the pump ends, and now and then while it waits cocked.
 */
public record PumpSteamDraw(float x, float y, float cellPixels, float progress) implements TowerEffectDraw {
}
