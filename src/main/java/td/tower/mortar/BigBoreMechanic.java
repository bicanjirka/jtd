package td.tower.mortar;

import td.term.TermText;
import td.tower.TowerTerm;

/**
 * Big Bore: Heavy shells take weight from Normals in the magazine's draw, so they come up more often in place of
 * Normals and leave a payload's share as it was.
 */
public final class BigBoreMechanic implements MortarMechanic {

    private final int shiftedWeight;

    public BigBoreMechanic(int shiftedWeight) {
        this.shiftedWeight = shiftedWeight;
    }

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withMagazine(spec.magazine().withWeight(ShellKind.HEAVY, this.shiftedWeight)
                .withWeight(ShellKind.NORMAL, -this.shiftedWeight));
    }

    @Override
    public TermText describe() {
        return TermText.of(TowerTerm.HEAVY_SHELL).plus(" shells come up more often, in place of Normals");
    }
}
