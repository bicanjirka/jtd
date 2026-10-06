package td.tower.sniper;

import td.tower.targeting.TargetQuery;

import java.util.Optional;

/**
 * What one owned node does to the Sniper's shots. A perk may reshape a shot before it is fired,
 * change how it counts once it has landed, and react to that outcome. Every hook does nothing by
 * default, so a perk implements only its own.
 */
public interface SniperPerk {

    default AimRules refineAim(AimRules rules) {
        return rules;
    }

    /** Who this perk makes the Sniper aim at; the first perk owned to say so decides. */
    default Optional<SniperAim> aim(Viewpoint view) {
        return Optional.empty();
    }

    /** The enemies the Sniper may shoot, widened beyond what {@code base} matches. */
    default TargetQuery widen(TargetQuery base, Viewpoint view) {
        return base;
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
