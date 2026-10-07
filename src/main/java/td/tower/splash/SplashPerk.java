package td.tower.splash;

/**
 * What one owned node does to the Splash. A perk may change how it blasts and what the blast
 * carries, reshape a shot before it is fired, and react to how it landed. Every hook does nothing
 * by default, so a perk implements only its own.
 */
public interface SplashPerk {

    /** How the Splash blasts and what the blast carries, as this perk changes it. */
    default SplashSpec refineSpec(SplashSpec spec) {
        return spec;
    }

    default SplashShot shape(SplashShot shot, ShotContext context) {
        return shot;
    }

    default void react(ShotResult result, SplashActions actions) {
    }
}
