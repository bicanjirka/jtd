package td.tower.mortar;

import td.term.TermText;
import td.tower.TowerTerm;

/** Rich Mix: a new shell is never the kind of the one in front of it. */
public final class RichMixMechanic implements MortarMechanic {

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withMagazine(spec.magazine().mixing());
    }

    @Override
    public TermText describe() {
        return TermText.of("a new shell in the ").plus(TowerTerm.MAGAZINE)
                .plus(" is never the same kind as the one in front of it");
    }
}
