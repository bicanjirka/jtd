package td.tower.sonar;

import td.effect.Effect;
import td.effect.EffectKind;
import td.stat.EnemyStat;

/** Fault Line: the resilience Resonant Crack takes falls further and doesn't come back while Exposed. */
public final class FaultLinePerk implements SonarPerk {

    @Override
    public SonarSpec refineSpec(SonarSpec spec) {
        return spec.withFaultLine();
    }

    @Override
    public String describe() {
        return "Resonant Crack's loss doesn't recover while Exposed, and falls to -"
                + Math.round(-EffectKind.FRACTURED.modifiersAt(1).flat(EnemyStat.RESILIENCE) * Effect.FAULT_LINE_STACK_CAP);
    }
}
