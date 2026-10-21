package td.tower.cinder;

import td.effect.EffectKind;
import td.term.Term;
import td.term.TermText;

import java.util.Set;

/** Flashpoint: a critical ignition also adds eight Scorched. */
public final class FlashpointMechanic implements CinderMechanic {

    private static final int STACKS = 8;

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withCritScorch(STACKS);
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.SCORCHED);
    }

    @Override
    public TermText describe() {
        return TermText.of("a crit ignition also adds " + STACKS + " stacks of ").plus(EffectKind.SCORCHED);
    }
}
