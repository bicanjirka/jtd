package td.tower.aura;

import td.tower.upgrade.PerkText;

/** Amplifying Core: Kinship also strengthens the buff for each other tower type. */
public final class KinshipStrengthPerk implements AuraPerk {

    private static final float STRENGTH_PER_TYPE = 0.05f;

    @Override
    public AuraSpec refineSpec(AuraSpec spec) {
        return spec.withKinship(spec.kinship().strengthening(STRENGTH_PER_TYPE));
    }

    @Override
    public String describe() {
        return "Kinship also strengthens the buff: +" + PerkText.percent(STRENGTH_PER_TYPE) + " per other tower type, up to "
                + KinshipRule.ATTUNED_TYPE_CAP;
    }
}
