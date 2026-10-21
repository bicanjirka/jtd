package td.tower.pulse;

import td.effect.EffectKind;
import td.stat.EnemyStat;
import td.term.Term;
import td.tower.upgrade.PerkText;
import td.util.TickRate;

import java.util.Set;

/** Arc Discharge: once a second a zap Condemns the strongest visible enemy in the field, a stack at a time. */
public final class ArcDischargePerk implements PulsePerk {

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
    public String describe() {
        int most = EffectKind.CONDEMNED.maxStacks();
        return "once a second a zap strikes the visible enemy of the highest rank inside, the furthest along of them: every "
                + "tower turns on it, and each zap Condemns it further, +" + PerkText.percent(perStack())
                + " damage taken from every source, up to +" + PerkText.percent(perStack() * most) + " at " + most
                + " zaps; both hold " + PerkText.seconds(MARK_TICKS) + " after the last zap";
    }

    private static float perStack() {
        return EffectKind.CONDEMNED.modifiersAt(1).flat(EnemyStat.PHYSICAL_DAMAGE_TAKEN);
    }
}
