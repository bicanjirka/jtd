package td.tower.sniper;

/**
 * What one owned node does to the Sniper. A perk may change whom it shoots and how Steady Aim
 * builds, reshape a shot before it is fired, change how it counts once it has landed, and react to
 * that outcome. Every hook does nothing by default, so a perk implements only its own.
 */
public interface SniperPerk {

    default SniperSpec refineSpec(SniperSpec spec) {
        return spec;
    }

    default SniperShot shape(SniperShot shot, ShotContext context) {
        return shot;
    }

    /** May change what the shot counts as, for the perks that follow. */
    default ShotResult settle(ShotResult result, ShotActions actions) {
        return result;
    }

    default void react(ShotResult result, ShotActions actions) {
    }
}
