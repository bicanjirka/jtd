package td.tower.splash;

/**
 * Mastery: strengthens the chain's levels II and III. On Arc, arcs crit more and Daze longer; on
 * Hex, Blight poisons half as hard again and a hexed enemy that dies leaves a cursed cloud.
 */
public final class MasteryPerk implements SplashPerk {

    private static final float ARC_CRIT_BONUS = 0.1f;
    private static final float BLIGHT_SHARE = 0.06f;

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        if (spec.arcs().active()) {
            return spec.withArcs(spec.arcs().withCritBonus(ARC_CRIT_BONUS).withLongDaze());
        }
        if (spec.hexes().isActive()) {
            return spec.withHexes(spec.hexes().withBlightShareAtLeast(BLIGHT_SHARE).withCursedCloud());
        }
        return spec;
    }
}
