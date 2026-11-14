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
     * Sends the shot that just landed on from {@code from} to the nearest other enemy within {@code reachCells}, if
     * there is one. The hop deals the shot's damage when it arrives and can't crit.
     */
    void ricochet(EnemyMob from, float reachCells);
}
