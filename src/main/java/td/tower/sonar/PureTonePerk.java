package td.tower.sonar;

import td.damage.DamageType;

/** Pure Tone: the beam deals magic damage instead of physical. */
public final class PureTonePerk implements SonarPerk {

    @Override
    public SonarStrike shape(SonarStrike strike, StrikeContext context) {
        return strike.withType(DamageType.MAGIC).withMagicShare(0f);
    }
}
