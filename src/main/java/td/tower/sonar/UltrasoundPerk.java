package td.tower.sonar;

import td.damage.DamageType;

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
        float protection = context.target().reductionAgainst(DamageType.PHYSICAL);
        return strike.withMagicShare(Math.max(BASE_SHARE, Math.min(MAX_SHARE, protection)));
    }
}
