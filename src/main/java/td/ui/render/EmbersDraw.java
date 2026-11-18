package td.ui.render;

/**
 * Red embers rising off a frenzied turret's top at ({@code x}, {@code y}, already lifted), on the tick clock's
 * {@code seconds}, so they stop on pause; {@code seed} keeps one tower's embers its own.
 */
public record EmbersDraw(float x, float y, float cellPixels, float seconds, int seed) implements TowerEffectDraw {
}
