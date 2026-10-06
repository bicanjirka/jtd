package td.tower.sniper;

import td.damage.DamageType;

/** Silver Rounds: every third shot is magic, and pierces some magic resist. */
public final class SilverRoundsPerk implements SniperPerk {

    private static final int INTERVAL = 3;
    private static final float MAGIC_PENETRATION = 0.25f;

    @Override
    public SniperShot shape(SniperShot shot, ShotContext context) {
        if (context.shotNumber() % INTERVAL != 0) {
            return shot;
        }
        return shot.withType(DamageType.MAGIC).withAttack(attack -> attack.withMagicPenetration(
                attack.magicPenetration() + MAGIC_PENETRATION, attack.magicPenetrationFlat()));
    }
}
