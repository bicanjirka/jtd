package td.tower.sonar;

import td.effect.Effect;
import td.effect.EffectKind;
import td.stat.EnemyStat;
import td.term.TermNumbers;
import td.term.TermText;

/** Fault Line: the resilience Resonant Crack takes falls further and doesn't come back while Exposed. */
public final class FaultLinePerk implements SonarPerk {

    @Override
    public SonarSpec refineSpec(SonarSpec spec) {
        return spec.withFaultLine();
    }

    @Override
    public TermText describe() {
        return TermText.of(EffectKind.FRACTURED)
                .plus(" doesn't wear off while the enemy has ").plus(EffectKind.EXPOSED)
                .plus(", and can reach "
                        + TermNumbers.signed(EffectKind.FRACTURED.modifiersAt(1).flat(EnemyStat.RESILIENCE)
                                * Effect.FAULT_LINE_STACK_CAP)
                        + " resilience");
    }
}
