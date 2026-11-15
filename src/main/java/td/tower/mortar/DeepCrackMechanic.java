package td.tower.mortar;

import td.effect.EffectKind;
import td.term.Term;
import td.term.TermText;

import java.util.Set;

/** Deep Crack: every shell's Crack cuts deeper. */
public final class DeepCrackMechanic implements MortarMechanic {

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withDeepCrack();
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.DEEP_CRACKED);
    }

    @Override
    public TermText describe() {
        return TermText.of("every shell applies ").plus(EffectKind.DEEP_CRACKED).plus(" in place of ")
                .plus(EffectKind.CRACKED);
    }
}
