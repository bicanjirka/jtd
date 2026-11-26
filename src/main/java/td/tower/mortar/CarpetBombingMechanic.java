package td.tower.mortar;

import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

/**
 * Carpet Bombing: the front shells are loaded together, a Normal sneaked between each two, and all of them laid along
 * the path behind the target.
 */
public final class CarpetBombingMechanic implements MortarMechanic {

    private static final int LOADED = 3;
    private static final float SPACING_CELLS = 1f;
    private static final float RELOAD_FACTOR = 1.9f;
    /** Its shells leave over the same span as a Barrage's, however many more of them there are. */
    private static final int GAP_TICKS = BarrageMechanic.GAP_TICKS * (BarrageMechanic.SHELLS - 1) / (2 * LOADED - 2);
    private static final VolleySpec VOLLEY = VolleySpec.alongThePath(LOADED, GAP_TICKS, RELOAD_FACTOR,
            SPACING_CELLS).withNormalsBetween();

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withVolley(VOLLEY);
    }

    @Override
    public TermText describe() {
        return TermText.of("fires ").plus(TowerTerm.CARPET_BOMBING).plus(": " + VOLLEY.fired() + " shells "
                + TermNumbers.cells(SPACING_CELLS) + " apart along the path, centred on the target, " + LOADED
                + " loaded and a Normal between each two; reloads " + TermNumbers.times(VOLLEY.reloadFactor())
                + " as long");
    }
}
