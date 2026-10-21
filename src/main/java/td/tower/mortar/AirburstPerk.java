package td.tower.mortar;

import td.term.TermNumbers;
import td.term.TermText;

/** Airburst: the blast hits at full damage across its inner half. */
public final class AirburstPerk implements MortarPerk {

    private static final float CORE_SHARE = 0.5f;

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withFlatCore(CORE_SHARE);
    }

    @Override
    public TermText describe() {
        return TermText.of("full damage across the inner " + TermNumbers.percent(CORE_SHARE) + " of the blast");
    }
}
