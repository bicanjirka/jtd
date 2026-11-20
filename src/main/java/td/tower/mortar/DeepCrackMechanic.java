package td.tower.mortar;

import td.effect.EffectKind;
import td.term.Term;
import td.term.TermText;

import java.util.Set;

/** Deep Crack: every shell's share of Crack is worth more stacks. */
public final class DeepCrackMechanic implements MortarMechanic {

    @Override
    public MortarSpec refineSpec(MortarSpec spec) {
        return spec.withCrackStacks(MortarSpec.DEEP_CRACK_STACKS);
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.CRACKED);
    }

    @Override
    public TermText describe() {
        return TermText.of("every shell's share of ").plus(EffectKind.CRACKED)
                .plus(" is worth " + MortarSpec.DEEP_CRACK_STACKS + " stacks");
    }
}
