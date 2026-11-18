package td.tower.sniper;

import td.enemy.EnemyMob;
import td.tower.upgrade.Mechanic;


/**
 * What one owned node does to the Sniper. A mechanic may reshape a shot before it is fired, change how it counts
 * once it has landed, and react to that outcome. Every hook does nothing by default, so a mechanic implements only its own.
 */
public interface SniperMechanic extends Mechanic {

    /** Reshapes the shot about to be fired at {@code target}. */
    default SniperShot shape(SniperShot shot, EnemyMob target) {
        return shot;
    }

    /** Whether it singles {@code target} out, which the rifle shows while it aims there. */
    default boolean hunts(EnemyMob target) {
        return false;
    }

    /** May change what the shot counts as, for the mechanics that follow. */
    default ShotResult settle(ShotResult result, ShotActions actions) {
        return result;
    }

    default void react(ShotResult result, ShotActions actions) {
    }

}
