package td.tower.cinder;

import td.effect.EffectKind;
import td.term.Term;
import td.term.TermNumbers;
import td.term.TermText;
import td.util.TickRate;

import java.util.Set;

/** Singe: the stream leaves what it touches Singed, the Scorcher's own debuff. */
public final class SingeMechanic implements CinderMechanic {

    /** The stream's Singe outlasts its last touch by this, so an enemy at the cone's edge keeps it as the head turns. */
    public static final int LINGER_TICKS = Math.round(TickRate.TICKS_PER_SECOND / 2f);

    @Override
    public CinderSpec refineSpec(CinderSpec spec) {
        return spec.withSinge();
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.SINGED);
    }

    @Override
    public TermText describe() {
        return TermText.of("its stream gives its own share of ").plus(EffectKind.SINGED)
                .plus(" while it touches, and " + TermNumbers.seconds(LINGER_TICKS / TickRate.TICKS_PER_SECOND) + " after");
    }
}
