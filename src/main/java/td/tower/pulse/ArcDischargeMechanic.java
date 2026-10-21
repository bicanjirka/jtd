package td.tower.pulse;

import td.effect.EffectKind;
import td.stat.EnemyStat;
import td.term.Term;
import td.term.TermNumbers;
import td.term.TermText;
import td.util.TickRate;

import java.util.Set;

/** Arc Discharge: once a second a zap Condemns the strongest visible enemy in the field, a stack at a time. */
public final class ArcDischargeMechanic implements PulseMechanic {

    private static final int MARK_TICKS = Math.round(3f * TickRate.TICKS_PER_SECOND);

    @Override
    public PulseSpec refineSpec(PulseSpec spec) {
        return spec.withZap(ZapSpec.marking(MARK_TICKS));
    }

    @Override
    public Set<Term> applies() {
        return Set.of(EffectKind.PRIORITY, EffectKind.CONDEMNED);
    }

    @Override
    public TermText describe() {
        return TermText.of("once a second a zap strikes the visible enemy of the highest rank inside, the furthest along of them: it gains ")
                .plus(EffectKind.PRIORITY).plus(" and a stack of ").plus(EffectKind.CONDEMNED)
                .plus(", up to " + EffectKind.CONDEMNED.maxStacks() + "; both hold " + TermNumbers.seconds(MARK_TICKS / (float) TickRate.TICKS_PER_SECOND)
                        + " after the last zap");
    }

    private static float perStack() {
        return EffectKind.CONDEMNED.modifiersAt(1).flat(EnemyStat.PHYSICAL_DAMAGE_TAKEN);
    }
}
