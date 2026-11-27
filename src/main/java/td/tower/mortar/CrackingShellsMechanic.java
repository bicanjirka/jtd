package td.tower.mortar;

import td.effect.EffectKind;
import td.term.Term;
import td.term.TermText;

import java.util.Set;

/** Every shell gives what its blast reaches a share of Crack. */
public final class CrackingShellsMechanic implements MortarMechanic {

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withCrackStacks(1);
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.CRACKED);
    }

    @Override
    public TermText describe() {
        return TermText.of("every shell gives what its blast reaches a share of a stack of ").plus(EffectKind.CRACKED);
    }
}
