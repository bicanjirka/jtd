package td.tower.mortar;

import td.effect.EffectKind;
import td.term.Term;
import td.term.TermNumbers;
import td.term.TermText;
import td.tower.TowerTerm;

import java.util.Set;

/** Fragmentation Rounds: a ring of shrapnel past the blast for a quarter of the damage, reaching further per step. */
public final class FragmentationPerk implements MortarPerk {

    private static final float DAMAGE_SHARE = 0.25f;
    private static final float REACH_CELLS_PER_STEP = 0.25f;

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withShrapnel(ShrapnelSpec.of(DAMAGE_SHARE, REACH_CELLS_PER_STEP));
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.CRACKED);
    }

    @Override
    public TermText describe() {
        return TermText.of("a ring of ").plus(TowerTerm.SHRAPNEL)
                .plus(" flies past the blast for " + TermNumbers.percent(DAMAGE_SHARE) + " damage and applies ")
                .plus(EffectKind.CRACKED)
                .plus(", reaching " + TermNumbers.cells(REACH_CELLS_PER_STEP) + " further per step of ")
                .plus(TowerTerm.BRACKETING);
    }
}
