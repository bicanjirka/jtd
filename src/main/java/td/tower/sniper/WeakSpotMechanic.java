package td.tower.sniper;

import td.term.TermText;

/** Weak Spot: a shot that is not a crit ignores plating, insulation and some armor. */
public final class WeakSpotMechanic implements SniperMechanic {

    private static final float ARMOR_IGNORED = 50f;
    private static final float FLAT_REDUCTION_IGNORED = 1f;

    @Override
    public SniperShot shape(SniperShot shot, ShotContext context) {
        return shot.withAttack(attack -> attack.withGlancingPenetration(ARMOR_IGNORED, FLAT_REDUCTION_IGNORED));
    }

    @Override
    public TermText describe() {
        return TermText.of("a shot that doesn't crit ignores plating, insulation and " + Math.round(ARMOR_IGNORED) + " armor");
    }
}
