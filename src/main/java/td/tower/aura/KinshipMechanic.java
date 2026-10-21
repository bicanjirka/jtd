package td.tower.aura;

import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

/** Attune's Kinship: buffed towers earn more XP for each other tower type in range. */
public final class KinshipMechanic implements AuraMechanic {

    @Override
    public AuraSpec refineSpec(AuraSpec spec) {
        return spec.withKinship(KinshipRule.attuned());
    }

    @Override
    public TermText describe() {
        return TermText.of(TowerTerm.KINSHIP)
                .plus(" makes buffed towers earn " + TermNumbers.signedPercent(KinshipRule.ATTUNED_XP_PER_TYPE)
                        + " XP for each other tower type in range, up to "
                        + TermNumbers.signedPercent(KinshipRule.ATTUNED_XP_PER_TYPE * KinshipRule.ATTUNED_TYPE_CAP));
    }
}
