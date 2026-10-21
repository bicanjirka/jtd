package td.tower.seeker;

import td.effect.EffectKind;
import td.term.Term;
import td.term.TermText;

import java.util.Set;

/** Brittle: a frozen enemy takes extra physical damage. */
public final class BrittlePerk implements SeekerPerk {

    @Override
    public SeekerSpec refineSpec(SeekerSpec spec) {
        return spec.withFreeze(spec.freeze().makingBrittle());
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.BRITTLE);
    }

    @Override
    public TermText describe() {
        return TermText.of("an enemy with ").plus(EffectKind.FREEZE).plus(" gains ").plus(EffectKind.BRITTLE);
    }
}
