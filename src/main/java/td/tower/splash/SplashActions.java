package td.tower.splash;

import td.enemy.EnemyMob;

/** What a perk may make the Splash do beyond shaping its shots. */
public interface SplashActions {

    /** Charges {@code target}: another tower's next hit on it discharges it, credited to this one. */
    void charge(EnemyMob target);
}
