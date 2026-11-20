package td.tower.mortar;

import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

/** Thick Mix: a Normal shell carries some of the payload, so its blast burns, poisons or chills what it reaches. */
public final class ThickMixMechanic implements MortarMechanic {

    static final float SHARE = 1f;

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withPayload(spec.payload().carriedByNormals(SHARE));
    }

    @Override
    public TermText describe() {
        return TermText.of("a Normal shell's blast carries " + TermNumbers.percent(SHARE) + " of a pulse of the ")
                .plus(TowerTerm.PAYLOAD).plus("'s ground");
    }
}
