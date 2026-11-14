package td.tower.splash;

import td.tower.upgrade.Mechanic;

/**
 * What one owned node does to the Burst. A mechanic may change what its passive, its Arc and its hexes are like, and,
 * when it brings an ability, cast it. Every hook does nothing by default, so a mechanic implements only its own.
 */
public interface SplashMechanic extends Mechanic {

    /** What the Burst's fork, Arc and hexes are like, as this mechanic changes them. */
    default SplashSpec refineSpec(SplashSpec spec) {
        return spec;
    }

    /** Casts this mechanic's ability through {@code actions}; whether it found a target and cast. */
    default boolean cast(CastActions actions) {
        return false;
    }
}
