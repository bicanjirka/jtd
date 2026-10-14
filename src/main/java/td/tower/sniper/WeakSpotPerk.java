package td.tower.sniper;

/** Weak Spot: a shot that is not a crit ignores plating, insulation and some armor. */
public final class WeakSpotPerk implements SniperPerk {

    private static final float ARMOR_IGNORED = 50f;
    private static final float FLAT_REDUCTION_IGNORED = 1f;

    @Override
    public SniperShot shape(SniperShot shot, ShotContext context) {
        return shot.withAttack(attack -> attack.withGlancingPenetration(ARMOR_IGNORED, FLAT_REDUCTION_IGNORED));
    }

    @Override
    public String describe() {
        return "Weak Spot: non-crit shots ignore plating, insulation and " + Math.round(ARMOR_IGNORED) + " armor";
    }
}
