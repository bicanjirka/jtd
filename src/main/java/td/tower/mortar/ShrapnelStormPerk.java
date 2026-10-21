package td.tower.mortar;

import td.effect.EffectKind;
import td.term.Term;
import td.term.TermText;

import java.util.Set;

/** Shrapnel Storm: what the shrapnel hits bleeds. */
public final class ShrapnelStormPerk implements MortarPerk {

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
        return TermText.of("what the shrapnel hits bleeds: physical damage for every cell it travels");
    }
}
