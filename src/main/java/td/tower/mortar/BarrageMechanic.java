package td.tower.mortar;

import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

/** Barrage: the front shells are loaded together and fired one after another at one target; the reload is longer. */
public final class BarrageMechanic implements MortarMechanic {

    static final int SHELLS = 3;
    static final int GAP_TICKS = 10;
    static final float RELOAD_FACTOR = 2.15f;

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withVolley(VolleySpec.atOneTarget(SHELLS, GAP_TICKS, RELOAD_FACTOR));
    }

    @Override
    public TermText describe() {
        return TermText.of("fires a ").plus(TowerTerm.BARRAGE).plus(" of " + SHELLS
                + " shells, a later one at a new target if its own died; reloads " + TermNumbers.times(RELOAD_FACTOR)
                + " as long");
    }
}
