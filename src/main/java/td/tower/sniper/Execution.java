package td.tower.sniper;

import td.enemy.EnemyMob;
import td.wave.Vec2;

/**
 * An enemy an execution killed.
 *
 * @param at    where it died, in pixels
 * @param tick  the tick it died
 * @param enemy the enemy it killed
 */
public record Execution(Vec2 at, int tick, EnemyMob enemy) {
}
