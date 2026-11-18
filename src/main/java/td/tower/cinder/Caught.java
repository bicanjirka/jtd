package td.tower.cinder;

import td.enemy.EnemyMob;

/**
 * An enemy the cone fed this tick.
 *
 * @param enemy         the enemy
 * @param fuelPerSecond what it was fed, in points a second
 * @param inCore        whether it stood in the Core
 */
public record Caught(EnemyMob enemy, float fuelPerSecond, boolean inCore) {
}
