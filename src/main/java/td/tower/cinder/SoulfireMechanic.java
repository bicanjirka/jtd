package td.tower.cinder;

import td.effect.EffectKind;
import td.term.Term;
import td.term.TermText;

import java.util.Set;

/** Soulfire: all its fire is Soulfire, which burns what is immune to Burn. */
public final class SoulfireMechanic implements CinderMechanic {

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withFire(EffectKind.SOULFIRE);
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.SOULFIRE);
    }

    @Override
    public TermText describe() {
        return TermText.of("all its fire is ").plus(EffectKind.SOULFIRE).plus(" in place of ").plus(EffectKind.BURN);
    }
}
