package td.tower.sonar;

import td.enemy.EnemyMob;

/**
 * What a perk may know about a beam hit it shapes.
 *
 * @param target     the enemy hit
 * @param rangeShare how far it is, as a share of the Sonar's range
 */
public record StrikeContext(EnemyMob target, float rangeShare) {
}
