package td.tower.splash;

import td.enemy.EnemyMob;

/**
 * One arc of a shot: where it jumped from and to, and how much of the blast it carries.
 *
 * @param from  the enemy it jumped from
 * @param to    the enemy it strikes
 * @param share its damage, a share of the blast's
 */
public record ArcStrike(EnemyMob from, EnemyMob to, float share) {
}
