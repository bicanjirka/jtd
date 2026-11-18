package td.tower.splash;

import td.wave.Vec2;

/**
 * A stone that burst.
 *
 * @param core    the radius it dealt full damage within, in pixels
 * @param edge    the radius its weaker edge reached, in pixels
 * @param tick when it landed
 */
public record Blast(Vec2 at, float core, float edge, int tick) {
}
