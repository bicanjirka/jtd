package td.tower.sniper;

import td.enemy.EnemyMob;

/**
 * What a perk may know about a shot it shapes.
 *
 * @param target     the enemy aimed at
 * @param lock       how long the Sniper has been aiming at it
 * @param shotNumber how many shots the Sniper has fired, this one included
 * @param rangeShare how far the target is, as a share of the Sniper's range
 */
public record ShotContext(EnemyMob target, AimLock lock, int shotNumber, float rangeShare) {

    public ShotContext(EnemyMob target, AimLock lock, int shotNumber) {
        this(target, lock, shotNumber, 0f);
    }
}
