package td.tower.splash;

/** Wide Charge: a wider blast whose edge still deals a quarter. */
public final class WideChargePerk implements SplashPerk {

    private static final float RADIUS_BONUS = 0.25f;
    private static final float EDGE_FLOOR = 0.25f;

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        return spec.withBlast(spec.blast().withRadiusBonus(RADIUS_BONUS).withEdgeFloor(EDGE_FLOOR));
    }
}
