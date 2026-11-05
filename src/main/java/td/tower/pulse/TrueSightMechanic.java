package td.tower.pulse;

import td.effect.EffectKind;
import td.term.Term;
import td.term.TermNumbers;
import td.term.TermText;
import td.util.TickRate;

import java.util.Set;

/** True Sight: the reveal lasts four seconds after leaving. */
public final class TrueSightMechanic implements PulseMechanic {

    private static final int REVEAL_TICKS = Math.round(4f * TickRate.TICKS_PER_SECOND);

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.withSight(spec.sight().revealing(REVEAL_TICKS));
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.REVEALED);
    }

    @Override
    public TermText describe() {
        return TermText.format("{} lasts " + TermNumbers.seconds(REVEAL_TICKS / (float) TickRate.TICKS_PER_SECOND) + " after an enemy leaves",
                EffectKind.REVEALED);
    }
}
