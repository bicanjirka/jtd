package td.tower.mortar;

import td.term.TermText;
import td.tower.TowerTerm;

/** Big Bore: Heavy shells weigh more in the magazine's draw, so they come up more often. */
public final class BigBoreMechanic implements MortarMechanic {

    private final int addedWeight;

    public BigBoreMechanic(int addedWeight) {
        this.addedWeight = addedWeight;
    }

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withMagazine(spec.magazine().withWeight(ShellKind.HEAVY, this.addedWeight));
    }

    @Override
    public TermText describe() {
        return TermText.of(TowerTerm.HEAVY_SHELL).plus(" shells come up more often");
    }
}
