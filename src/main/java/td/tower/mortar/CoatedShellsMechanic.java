package td.tower.mortar;

import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

/**
 * Coated Shells: a Normal shell right behind a payload carries some of it: its blast stays a physical Normal's hit, and
 * puts a share of a pulse of that payload's ground on what it reaches.
 */
public final class CoatedShellsMechanic implements MortarMechanic {

    static final float SHARE = 2.5f;

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
