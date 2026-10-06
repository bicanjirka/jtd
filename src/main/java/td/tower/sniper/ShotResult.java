package td.tower.sniper;

import td.enemy.EnemyMob;

/**
 * How a shot landed on the enemy it was aimed at.
 *
 * @param target   the enemy aimed at
 * @param critical whether it counts as a crit for whatever triggers off one
 * @param killed   whether the enemy died
 */
public record ShotResult(EnemyMob target, boolean critical, boolean killed) {
}
