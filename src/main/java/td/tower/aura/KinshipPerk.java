package td.tower.aura;

/** Attune's Kinship: buffed towers earn more XP for each other tower type in range. */
public final class KinshipPerk implements AuraPerk {

    @Override
    public AuraSpec refineSpec(AuraSpec spec) {
        return spec.withKinship(KinshipRule.attuned());
    }
}
