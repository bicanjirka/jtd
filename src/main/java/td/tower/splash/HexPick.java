package td.tower.splash;

import td.enemy.EnemyMob;

/**
 * The hex a cast puts on, and on whom.
 *
 * @param hex    the hex whose turn it is
 * @param target the enemy its rule picked
 */
public record HexPick(Hex hex, EnemyMob target) {
}
