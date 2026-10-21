package td.tower.cinder;

import td.effect.EffectKind;
import td.term.Term;
import td.term.TermText;
import td.tower.TowerTerm;

import java.util.Set;

/** Soulfire: every wave also burns as Soulfire, a blue burn in a pool of its own beside the ordinary one. */
public final class SoulfirePerk implements CinderPerk {

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withSoulfire();
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.SOULFIRE);
    }

    @Override
    public TermText describe() {
        return TermText.of("every wave also applies ").plus(EffectKind.SOULFIRE).plus(" beside ").plus(EffectKind.BURN)
                .plus(", at the same strength; ").plus(TowerTerm.STOKE).plus(" raises both");
    }
}
