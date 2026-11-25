package td.tower.mortar;

import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

/**
 * Thick Mix: a Normal shell right behind a payload carries some of it, so its blast burns, poisons or chills what it
 * reaches.
 */
public final class ThickMixMechanic implements MortarMechanic {

    static final float SHARE = 1f;

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withPayload(spec.payload().carriedByNormals(SHARE));
    }

    @Override
    public TermText describe() {
        return TermText.of("a Normal shell right behind a ").plus(TowerTerm.PAYLOAD)
                .plus(" shell carries " + TermNumbers.percent(SHARE) + " of a pulse of its ground in its blast");
    }
}
