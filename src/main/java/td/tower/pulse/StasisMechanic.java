package td.tower.pulse;

import td.effect.EffectKind;
import td.term.Term;
import td.term.TermNumbers;
import td.term.TermText;
import td.util.TickRate;

import java.util.Set;

/** Stasis: an enemy reaching full Toll is Dazed, and pays for it in Toll while the Daze holds. */
public final class StasisMechanic implements PulseMechanic {

    private static final int STASIS_TICKS = Math.round(1f * TickRate.TICKS_PER_SECOND);
    private static final int TICKS_PER_TOLL = Math.round(0.25f * TickRate.TICKS_PER_SECOND);

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.withHold(spec.hold().stilling(STASIS_TICKS, TICKS_PER_TOLL));
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.DAZED, EffectKind.STILLED);
    }

    @Override
    public TermText describe() {
        return TermText.of("an enemy at full ").plus(EffectKind.TOLL).plus(" gains ").plus(EffectKind.DAZED)
                .plus(" for " + TermNumbers.seconds(STASIS_TICKS / (float) TickRate.TICKS_PER_SECOND) + " and ").plus(EffectKind.STILLED)
                .plus(": while both hold, it loses a stack of ").plus(EffectKind.TOLL)
                .plus(" every " + TermNumbers.seconds(TICKS_PER_TOLL / (float) TickRate.TICKS_PER_SECOND) + "; a ").plus(EffectKind.DAZED)
                .plus(" that doesn't take hold costs nothing and is tried again a second later");
    }
}
