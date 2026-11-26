package td.tower.mortar;

import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

/**
 * Thick Mix: a Normal shell right behind a payload carries some of it: its blast stays a physical Normal's hit, and
 * puts a share of a pulse of that payload's ground on what it reaches.
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
                .plus(" shell carries some of it: its blast still hits as a physical Normal, and puts "
                        + TermNumbers.number(SHARE) + " pulses of that payload's ground on what it reaches");
    }
}
