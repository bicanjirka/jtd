package td.tower.aura;

import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

/** Amplifying Core: Kinship also strengthens the buff for each other tower type. */
public final class KinshipStrengthMechanic implements AuraMechanic {

    private static final float STRENGTH_PER_TYPE = 0.05f;

    @Override
    public AuraSpec refineSpec(AuraSpec spec) {
        return spec.withKinship(spec.kinship().strengthening(STRENGTH_PER_TYPE));
    }

    @Override
    public TermText describe() {
        return TermText.of(TowerTerm.KINSHIP)
                .plus(" also strengthens the buff: " + TermNumbers.signedPercent(STRENGTH_PER_TYPE)
                        + " per other tower type, up to " + KinshipRule.ATTUNED_TYPE_CAP + " types");
    }
}
