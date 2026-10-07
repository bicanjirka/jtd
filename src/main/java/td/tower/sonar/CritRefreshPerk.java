package td.tower.sonar;

import td.effect.Effect;
import td.effect.EffectKind;

/** A beam crit on an Exposed enemy keeps it Exposed as long as a fresh ping would. */
public final class CritRefreshPerk implements SonarPerk {

    @Override
    public void react(StrikeResult result, SonarActions actions) {
        if (result.critical() && !result.killed() && result.target().hasEffect(EffectKind.EXPOSED)) {
            actions.apply(result.target(), sink -> Effect.exposed(result.spec().pingTicks(), sink));
        }
    }
}
