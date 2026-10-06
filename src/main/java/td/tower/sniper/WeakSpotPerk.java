package td.tower.sniper;

/** Weak Spot: a shot that is not a crit ignores plating and some armor. */
public final class WeakSpotPerk implements SniperPerk {

    private static final float ARMOR_IGNORED = 50f;

    @Override
    public SniperShot shape(SniperShot shot, ShotContext context) {
        return shot.withAttack(attack -> attack.withGlancingPenetration(ARMOR_IGNORED, 1f));
    }
}
