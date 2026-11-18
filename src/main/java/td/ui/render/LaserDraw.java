package td.ui.render;

/**
 * A hunting scope's laser from its lens at ({@code fromX}, {@code fromY}) to the enemy it singles out at
 * ({@code toX}, {@code toY}); it shimmers on the wall clock's {@code seconds}, as a sight and not an event.
 */
public record LaserDraw(float fromX, float fromY, float toX, float toY, float cellPixels, float seconds)
        implements TowerEffectDraw {
}
