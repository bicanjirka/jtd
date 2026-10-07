package td.tower.seeker;

import td.enemy.EnemyMob;

/**
 * How a missile landed.
 *
 * @param target  the enemy it reached
 * @param before  what the target was suffering when it hit
 * @param froze   whether it froze a target that was not frozen before
 * @param rearmed whether the missile was itself launched by a rearm
 */
public record Impact(EnemyMob target, Before before, boolean froze, boolean rearmed) {

    /** What an enemy was suffering as a missile hit it. */
    public enum Before {
        NOTHING,
        CHILLED,
        FROZEN
    }

    /** Whether the target was already frozen when it hit. */
    public boolean wasFrozen() {
        return this.before == Before.FROZEN;
    }

    /** Whether the target was already frozen or chilled when it hit. */
    public boolean wasControlled() {
        return this.before != Before.NOTHING;
    }
}
