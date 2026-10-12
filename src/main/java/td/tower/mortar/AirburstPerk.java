package td.tower.mortar;

/** Airburst: the blast hits at full damage across its inner half. */
public final class AirburstPerk implements MortarPerk {

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withFlatCore();
    }
}
