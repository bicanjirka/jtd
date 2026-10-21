package td.tower.mortar;

import td.effect.EffectKind;
import td.term.Term;
import td.term.TermText;
import td.tower.TowerTerm;

import java.util.Set;

/** Shrapnel Storm: what the shrapnel hits bleeds. */
public final class ShrapnelStormMechanic implements MortarMechanic {

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withShrapnel(spec.shrapnel().bleeding());
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.BLEEDING);
    }

    @Override
    public TermText describe() {
        return TermText.of("an enemy ").plus(TowerTerm.SHRAPNEL).plus(" hits gains ").plus(EffectKind.BLEEDING);
    }
}
