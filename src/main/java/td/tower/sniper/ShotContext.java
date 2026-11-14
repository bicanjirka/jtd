package td.tower.sniper;

import td.enemy.EnemyMob;

/**
 * What a mechanic may know about a shot it shapes.
 *
 * @param target the enemy aimed at
 */
public record ShotContext(EnemyMob target) {
}
