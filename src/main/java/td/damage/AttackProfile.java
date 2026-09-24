package td.damage;

/**
 * The attacker's side of a hit: crit chance and multiplier, and penetration against armor and magic
 * resist. It travels with the hit, so whoever lands it (a projectile, a burn tick) never reads it
 * back from the tower.
 * <p>
 * Penetration only lowers positive mitigation, percentage first, then flat, and never below zero.
 *
 * @param critChance              chance in {@code [0, 1]} that the hit is critical
 * @param critMultiplier          a critical hit's damage multiplier
 * @param armorPenetration        fraction of positive armor ignored
 * @param armorPenetrationFlat    armor points ignored after the fraction
 * @param magicPenetration        fraction of positive magic resist ignored
 * @param magicPenetrationFlat    magic resist points ignored after the fraction
 */
public record AttackProfile(float critChance, float critMultiplier, float armorPenetration,
                            float armorPenetrationFlat, float magicPenetration, float magicPenetrationFlat) {

    public static final float DEFAULT_CRIT_MULTIPLIER = 1.5f;

    private static final AttackProfile NONE = new AttackProfile(0f, DEFAULT_CRIT_MULTIPLIER, 0f, 0f, 0f, 0f);

    /** Never crits and penetrates nothing. */
    public static AttackProfile none() {
        return NONE;
    }

    public static AttackProfile critChance(float critChance) {
        return NONE.withCritChance(critChance);
    }

    public AttackProfile withCritChance(float critChance) {
        return new AttackProfile(critChance, this.critMultiplier, this.armorPenetration, this.armorPenetrationFlat,
                this.magicPenetration, this.magicPenetrationFlat);
    }

    public AttackProfile withCritMultiplier(float critMultiplier) {
        return new AttackProfile(this.critChance, critMultiplier, this.armorPenetration, this.armorPenetrationFlat,
                this.magicPenetration, this.magicPenetrationFlat);
    }

    public AttackProfile withArmorPenetration(float fraction, float flat) {
        return new AttackProfile(this.critChance, this.critMultiplier, fraction, flat, this.magicPenetration,
                this.magicPenetrationFlat);
    }

    public AttackProfile withMagicPenetration(float fraction, float flat) {
        return new AttackProfile(this.critChance, this.critMultiplier, this.armorPenetration,
                this.armorPenetrationFlat, fraction, flat);
    }

    /** {@code mitigation} after this attacker's penetration for {@code type}. */
    public float penetrate(DamageType type, float mitigation) {
        if (mitigation <= 0f) {
            return mitigation;
        }
        float fraction = type == DamageType.PHYSICAL ? this.armorPenetration : this.magicPenetration;
        float flat = type == DamageType.PHYSICAL ? this.armorPenetrationFlat : this.magicPenetrationFlat;
        return Math.max(0f, mitigation * (1f - fraction) - flat);
    }
}
