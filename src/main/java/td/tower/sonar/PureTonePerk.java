package td.tower.sonar;

import td.damage.DamageType;

/** Pure Tone: the beam deals magic damage instead of physical, and pierces some magic resist. */
public final class PureTonePerk implements SonarPerk {

    private static final float MAGIC_PENETRATION = 0.15f;

    @Override
    public SonarStrike shape(SonarStrike strike, StrikeContext context) {
        return strike.withType(DamageType.MAGIC).withMagicShare(0f).withAttack(attack -> attack
                .withMagicPenetration(attack.magicPenetration() + MAGIC_PENETRATION, attack.magicPenetrationFlat()));
    }
}
