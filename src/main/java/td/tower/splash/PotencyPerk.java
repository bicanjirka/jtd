package td.tower.splash;

import td.tower.upgrade.PerkText;

/**
 * Potency: strengthens the chain's level I. On Arc, one more jump and arcs at 65% of the blast; on
 * Hex, Doom pays out 45%.
 */
public final class PotencyPerk implements SplashPerk {

    private static final float ARC_SHARE = 0.65f;
    private static final float DOOM_SHARE = 0.45f;
    private static final int MORE_JUMPS = 1;

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        if (spec.arcs().active()) {
            return spec.withArcs(spec.arcs().withMoreJumps(MORE_JUMPS).withShareAtLeast(ARC_SHARE));
        }
        if (spec.hexes().isActive()) {
            return spec.withHexes(spec.hexes().withDoomShareAtLeast(DOOM_SHARE));
        }
        return spec;
    }

    @Override
    public String describe() {
        return "Stormcaller: +" + MORE_JUMPS + " jump, arcs deal " + PerkText.percent(ARC_SHARE) + "; Hexer: Doom pays out "
                + PerkText.percent(DOOM_SHARE);
    }
}
