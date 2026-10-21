package td.tower.sonar;

import td.damage.DamageType;
import td.effect.EffectKind;
import td.term.TermNumbers;
import td.term.TermText;

/**
 * Ultrasound: part of every physical hit is added on as magic, more against an enemy that shrugs
 * off physical damage.
 */
public final class UltrasoundPerk implements SonarPerk {

    private static final float BASE_SHARE = 0.2f;
    private static final float MAX_SHARE = 0.5f;

    @Override
    public SonarStrike shape(SonarStrike strike, StrikeContext context) {
        if (strike.type() != DamageType.PHYSICAL) {
            return strike;
        }
        float protection = context.target().hasEffect(EffectKind.SHIELD) ? MAX_SHARE
                : context.target().reductionAgainst(DamageType.PHYSICAL);
        return strike.withMagicShare(Math.clamp(protection, BASE_SHARE, MAX_SHARE));
    }

    @Override
    public TermText describe() {
        return TermText.of(TermNumbers.percent(BASE_SHARE) + " of each hit is added as magic, up to "
                + TermNumbers.percent(MAX_SHARE) + " against an enemy with armor or ").plus(EffectKind.SHIELD);
    }
}
