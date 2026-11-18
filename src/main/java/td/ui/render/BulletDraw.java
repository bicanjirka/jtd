package td.ui.render;

/**
 * A bullet of {@code round} {@code flown} 0..1 of the way from ({@code fromX}, {@code fromY}) to ({@code toX},
 * {@code toY}), its streak trailing behind its head; a ricochet's path bows {@code bow} of its length to one side, a
 * straight shot's not at all. {@code alpha} fades a trace that has landed; {@code seconds} is the tick clock a Silver
 * Round's glitter follows.
 */
public record BulletDraw(BulletRound round, float fromX, float fromY, float toX, float toY, float bow, float flown,
                         float alpha, float cellPixels, float seconds) implements TowerEffectDraw {
}
