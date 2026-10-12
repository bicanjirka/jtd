package td.tower.proc;

import td.enemy.EnemyMob;

/**
 * How one landed attack went, as the tower settled it.
 *
 * @param target   whom it landed on
 * @param kind     what kind of hit it was
 * @param critical whether it was critical
 * @param killed   whether it killed
 */
public record AttackOutcome(EnemyMob target, HitKind kind, boolean critical, boolean killed) {
}
