package td.tower.mortar;

import td.tower.upgrade.PerkText;

/** Airburst: the blast hits at full damage across its inner half. */
public final class AirburstPerk implements MortarPerk {

    private static final float CORE_SHARE = 0.5f;

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withFlatCore(CORE_SHARE);
    }

    @Override
    public String describe() {
        return "full damage across the inner " + PerkText.percent(CORE_SHARE) + " of the blast";
    }
}
