package td.tower.splash;

/**
 * What one owned node does to the Splash. Every hook does nothing by default, so a perk implements
 * only its own.
 */
public interface SplashPerk {

    /** How the Splash blasts and what the blast carries, as this perk changes it. */
    default SplashSpec refineSpec(SplashSpec spec) {
        return spec;
    }
}
