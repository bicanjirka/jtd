package td.tower.seeker;

import td.enemy.EnemyMob;

/**
 * How a missile landed.
 *
 * @param target    the enemy it reached
 * @param wasFrozen whether the target was already frozen when it hit
 * @param froze     whether it froze a target that was not frozen before
 * @param rearmed   whether the missile was itself launched by a rearm
 */
public record Impact(EnemyMob target, boolean wasFrozen, boolean froze, boolean rearmed) {
}
