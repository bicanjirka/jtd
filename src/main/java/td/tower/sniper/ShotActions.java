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
     * Makes {@code target} Bleed for {@code ticks}: for every cell it walks, {@code sharePerCell} of what a crit of the
     * shot that just landed deals at the stats it was fired with, past armor and plating.
     */
    void bleed(EnemyMob target, float sharePerCell, int ticks);

    /** Exposes {@code target} for {@code ticks}: the Sniper's own share, refreshed rather than added to. */
    void expose(EnemyMob target, int ticks);

    /**
     * Sends the shot that just landed on from {@code from} to another enemy within {@code reachCells} of it, picked at
     * random, if there is one its shot has not struck yet and it has hopped fewer than {@code maxHops} times. The hop
     * is a shot of its own, with the stats the shot was fired with: shaped for the enemy it flies to, and settled and
     * reported when it lands.
     */
    void ricochet(EnemyMob from, float reachCells, int maxHops);
}
