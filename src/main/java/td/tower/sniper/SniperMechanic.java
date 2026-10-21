package td.tower.sniper;

import td.tower.upgrade.Mechanic;


/**
 * What one owned node does to the Sniper. A mechanic may change whom it shoots, reshape a shot before it is fired, change how it counts once it has landed, and react to
 * that outcome. Every hook does nothing by default, so a mechanic implements only its own.
 */
public interface SniperMechanic extends Mechanic {

    default SniperSpec refineSpec(SniperSpec spec) {
        return spec;
    }

    default SniperShot shape(SniperShot shot, ShotContext context) {
        return shot;
    }

    /** May change what the shot counts as, for the mechanics that follow. */
    default ShotResult settle(ShotResult result, ShotActions actions) {
        return result;
    }

    default void react(ShotResult result, ShotActions actions) {
    }

}
