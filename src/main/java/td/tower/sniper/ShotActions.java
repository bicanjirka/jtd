package td.tower.sniper;

import td.enemy.EnemyMob;

/** What a perk may make the Sniper do beyond shaping its shot. */
public interface ShotActions {

    void startFrenzy();

    void startBurst();

    /**
     * Kills {@code target} outright, credited to the Sniper, and tells whether it died. It is not a
     * hit: it never crits and spends no mark.
     */
    boolean execute(EnemyMob target);

    void applyVulnerable(EnemyMob target, int stacks);

    void applySundered(EnemyMob target, int stacks);
}
