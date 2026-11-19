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
     * Makes {@code target} Bleed for {@code ticks}: for every cell it walks, {@code sharePerCell} of the damage the
     * shot that just landed was fired with, past armor and plating.
     */
    void bleed(EnemyMob target, float sharePerCell, int ticks);

    /** Exposes {@code target} for {@code ticks}: the Sniper's own share, refreshed rather than added to. */
    void expose(EnemyMob target, int ticks);

    /**
     * Sends the shot that just landed on from {@code from} to another enemy within {@code reachCells} of it, picked at
     * random, if there is one. The hop deals the shot's damage when it arrives and may crit.
     */
    void ricochet(EnemyMob from, float reachCells);
}
