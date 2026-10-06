package td.tower.sniper;

import td.enemy.EnemyMob;

/**
 * What a perk may know about a shot it shapes.
 *
 * @param target     the enemy aimed at
 * @param lock       how long the Sniper has been aiming at it
 * @param shotNumber how many shots the Sniper has fired, this one included
 */
public record ShotContext(EnemyMob target, AimLock lock, int shotNumber) {
}
