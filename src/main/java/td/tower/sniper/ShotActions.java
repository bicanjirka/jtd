package td.tower.sniper;

import td.enemy.EnemyMob;

/** What a mechanic may make the Sniper do beyond shaping its shot. */
public interface ShotActions {

    /**
     * Kills {@code target} outright, credited to the Sniper, and tells whether it died. It is not a
     * hit: it never crits and spends no mark.
     */
    boolean execute(EnemyMob target);

    /**
     * Bounces the shot that just landed from {@code from} to the nearest enemy next to it, up to
     * {@code bounces} times, each for {@code share} of the Sniper's damage; each bounce may crit.
     * "Next to" is within {@code reachCells} of the enemy last struck.
     */
    void ricochet(EnemyMob from, int bounces, float share, float reachCells);
}
