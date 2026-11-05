package td.tower.seeker;

import td.effect.EffectKind;
import td.term.Term;
import td.term.TermText;

import java.util.Set;

/** Brittle: a freeze that lands also leaves the enemy taking extra physical damage. */
public final class BrittleMechanic implements SeekerMechanic {

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
        return TermText.of("an enemy it gives ").plus(EffectKind.FREEZE).plus(" gains ").plus(EffectKind.BRITTLE)
                .plus(" for as long");
    }
}
