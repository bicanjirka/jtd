package td.tower.mortar;

import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

/** Carpet Bombing: the front shells are loaded together and laid along the path behind the target. */
public final class CarpetBombingMechanic implements MortarMechanic {

    private static final float SPACING_CELLS = 1.5f;

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withVolley(VolleySpec.alongThePath(BarrageMechanic.SHELLS, BarrageMechanic.GAP_TICKS,
                BarrageMechanic.RELOAD_FACTOR, SPACING_CELLS));
    }

    @Override
    public TermText describe() {
        return TermText.of("fires ").plus(TowerTerm.CARPET_BOMBING).plus(": " + BarrageMechanic.SHELLS + " shells "
                + TermNumbers.cells(SPACING_CELLS) + " apart along the path; reloads "
                + TermNumbers.times(BarrageMechanic.RELOAD_FACTOR) + " as long");
    }
}
