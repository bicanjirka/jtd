package td.tower.splash;

import td.tower.BehaviourLine;
import td.tower.upgrade.Perk;

import java.util.Optional;

/**
 * What one owned node does to the Splash. A perk may change how it blasts and what the blast
 * carries, reshape a shot before it is fired, and react to how it landed. Every hook does nothing
 * by default, so a perk implements only its own.
 */
public interface SplashPerk extends Perk {

    /** How the Splash blasts and what the blast carries, as this perk changes it. */
    default SplashSpec refineSpec(SplashSpec spec) {
        return spec;
    }

    default SplashShot shape(SplashShot shot, ShotContext context) {
        return shot;
    }

    default void react(ShotResult result) {
    }

    /** The row this perk adds to the tower's info, when it does something no other row says. */
    default Optional<BehaviourLine> behaviour() {
        return Optional.empty();
    }
}
