package td.tower.aura;

import td.tower.upgrade.PerkText;

/** Attune's Kinship: buffed towers earn more XP for each other tower type in range. */
public final class KinshipPerk implements AuraPerk {

    @Override
    public AuraSpec refineSpec(AuraSpec spec) {
        return spec.withKinship(KinshipRule.attuned());
    }

    @Override
    public String describe() {
        return "Kinship: buffed towers earn +" + PerkText.percent(KinshipRule.ATTUNED_XP_PER_TYPE)
                + " XP for each other tower type in range, up to +"
                + PerkText.percent(KinshipRule.ATTUNED_XP_PER_TYPE * KinshipRule.ATTUNED_TYPE_CAP);
    }
}
