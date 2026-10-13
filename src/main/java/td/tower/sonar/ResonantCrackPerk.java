package td.tower.sonar;

import td.effect.ActiveEffects;
import td.effect.Effect;
import td.effect.EffectKind;
import td.stat.EnemyStat;
import td.tower.upgrade.PerkText;

/** Resonant Crack: every hit takes resilience, which comes back a little at a time. */
public final class ResonantCrackPerk implements SonarPerk {

    private static final int PER_STACK = Math.round(-EffectKind.FRACTURED.modifiersAt(1).flat(EnemyStat.RESILIENCE));

    @Override
    public void react(StrikeResult result, SonarActions actions) {
        if (result.killed()) {
            return;
        }
        boolean faultLine = result.spec().faultLine();
        actions.apply(result.target(), sink -> faultLine ? Effect.fractured(1, sink).withFaultLine()
                : Effect.fractured(1, sink));
    }

    @Override
    public String describe() {
        return "each beam hit takes " + PER_STACK + " resilience, down to -" + PER_STACK * EffectKind.FRACTURED.maxStacks() + ", back "
                + PER_STACK + " every " + PerkText.seconds(ActiveEffects.STACK_DECAY_INTERVAL_TICKS);
    }
}
