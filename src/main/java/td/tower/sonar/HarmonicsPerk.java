package td.tower.sonar;

import td.effect.EffectKind;

/** Harmonics: a hit makes the enemy take more magic damage. */
public final class HarmonicsPerk implements SonarPerk {

    @Override
    public void react(StrikeResult result, SonarActions actions) {
        if (!result.killed()) {
            actions.applyStacks(result.target(), EffectKind.RESONATING, 1);
        }
    }
}
