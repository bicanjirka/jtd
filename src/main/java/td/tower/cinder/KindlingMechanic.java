package td.tower.cinder;

import td.effect.EffectKind;
import td.term.Term;
import td.term.TermText;

import java.util.Set;

/** Kindling: Sear gives more Scorched, for the crits of the towers around it. */
public final class KindlingMechanic implements CinderMechanic {

    private static final int SEAR_STACKS = 10;

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withSearStacks(SEAR_STACKS);
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.SCORCHED);
    }

    @Override
    public TermText describe() {
        return TermText.of("the first touch of each stream gives " + SEAR_STACKS + " stacks of ").plus(EffectKind.SCORCHED);
    }
}
