package td.tower.mortar;

import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

/** Fragmentation Rounds II: the shrapnel does 30% more of everything and carries its shell's own effect. */
public final class ShrapnelBoostMechanic implements MortarMechanic {

    private static final float FACTOR = 1.3f;

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withShrapnel(spec.shrapnel().scaledBy(FACTOR).carryingTheShellsEffect());
    }

    @Override
    public TermText describe() {
        return TermText.of(TowerTerm.SHRAPNEL)
                .plus(" does " + TermNumbers.percent(FACTOR - 1f)
                        + " more of everything it does, and carries its shell's own effect");
    }
}
