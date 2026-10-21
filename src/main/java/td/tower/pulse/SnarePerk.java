package td.tower.pulse;

import td.effect.EffectKind;
import td.term.Term;
import td.term.TermNumbers;
import td.term.TermText;
import td.util.TickRate;

import java.util.Set;

/** Snare: an enemy is Dazed when it gains its first Toll stack of a visit. */
public final class SnarePerk implements PulsePerk {

    private static final int DAZE_TICKS = Math.round(0.5f * TickRate.TICKS_PER_SECOND);

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.withVisit(spec.visit().dazing(DAZE_TICKS));
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.DAZED);
    }

    @Override
    public TermText describe() {
        return TermText.of("an enemy gains ").plus(EffectKind.DAZED)
                .plus(" for " + TermNumbers.seconds(DAZE_TICKS / (float) TickRate.TICKS_PER_SECOND) + " with its first stack of ").plus(EffectKind.TOLL)
                .plus(", once per visit");
    }
}
