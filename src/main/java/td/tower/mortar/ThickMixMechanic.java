package td.tower.mortar;

import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

/** Thick Mix: a payload shell's blast reaches further, and its ground with it. */
public final class ThickMixMechanic implements MortarMechanic {

    private static final float EXTRA_CELLS = 0.5f;

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withPayload(spec.payload().wider(EXTRA_CELLS));
    }

    @Override
    public TermText describe() {
        return TermText.of("a ").plus(TowerTerm.PAYLOAD).plus(" shell's blast reaches "
                + TermNumbers.cells(EXTRA_CELLS) + " further, and its ground with it");
    }
}
