package td.tower.sonar;

import td.effect.Effect;

/** Resonant Crack: every hit takes resilience, which comes back a little at a time. */
public final class ResonantCrackPerk implements SonarPerk {

    @Override
    public void react(StrikeResult result, SonarActions actions) {
        if (result.killed()) {
            return;
        }
        boolean faultLine = result.spec().faultLine();
        actions.apply(result.target(), sink -> faultLine ? Effect.fractured(1, sink).withFaultLine()
                : Effect.fractured(1, sink));
    }
}
