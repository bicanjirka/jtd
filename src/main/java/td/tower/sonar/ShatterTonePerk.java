package td.tower.sonar;

import td.effect.EffectKind;

/** Shatter Tone: a hit on a shielded enemy breaks part of its shield. */
public final class ShatterTonePerk implements SonarPerk {

    private static final float SHARE = 0.25f;

    @Override
    public void react(StrikeResult result, SonarActions actions) {
        if (!result.killed() && result.target().hasEffect(EffectKind.SHIELD)) {
            actions.breakShield(result.target(), SHARE);
        }
    }
}
