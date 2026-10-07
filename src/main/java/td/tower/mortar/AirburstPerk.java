package td.tower.mortar;

/** Airburst: the blast is 25% wider and hits at full damage across its inner half. */
public final class AirburstPerk implements MortarPerk {

    private static final float FACTOR = 1.25f;

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withBlastScaledBy(FACTOR).withFlatCore();
    }
}
