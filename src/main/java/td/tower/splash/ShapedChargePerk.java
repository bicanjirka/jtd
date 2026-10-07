package td.tower.splash;

/** Shaped Charge: enemies in the inner half gain two Saturation stacks, and Saturation caps at four. */
public final class ShapedChargePerk implements SplashPerk {

    private static final int INNER_STACKS = 2;
    private static final int CAP = 4;

    @Override
    public SplashSpec refineSpec(SplashSpec spec) {
        SaturationRule saturation = spec.blast().saturation().withInnerPerHit(INNER_STACKS).withCap(CAP);
        return spec.withBlast(spec.blast().withSaturation(saturation));
    }
}
