package td.damage;

/**
 * The attacker's side of a hit: crit chance and multiplier, and penetration against armor, magic
 * resist and plating. It travels with the hit, so whoever lands it (a projectile, a burn tick) never
 * reads it back from the tower.
 * <p>
 * Penetration only lowers positive mitigation, percentage first, then flat, and never below zero.
 *
 * @param critChance              chance in {@code [0, 1]} that the hit is critical
 * @param critMultiplier          a critical hit's damage multiplier
 * @param armorPenetration        fraction of positive armor ignored
 * @param armorPenetrationFlat    armor points ignored after the fraction
 * @param magicPenetration        fraction of positive magic resist ignored
 * @param magicPenetrationFlat    magic resist points ignored after the fraction
 * @param platingPenetration      fraction of plating ignored, either type
 * @param delivery                whether this is a hit, which can crit, or periodic damage, which never does
 * @param guaranteedCrit          a hit that crits whenever the target's resilience is below 100
 * @param glancingArmorPenetrationFlat extra armor and magic resist points ignored when the hit is not critical
 * @param glancingPlatingPenetration   fraction of plating ignored when the hit is not critical
 */
public record AttackProfile(float critChance, float critMultiplier, float armorPenetration,
                            float armorPenetrationFlat, float magicPenetration, float magicPenetrationFlat,
                            float platingPenetration, Delivery delivery, boolean guaranteedCrit,
                            float glancingArmorPenetrationFlat, float glancingPlatingPenetration) {

    public static final float DEFAULT_CRIT_MULTIPLIER = 1.5f;

    private static final AttackProfile NONE = new AttackProfile(0f, DEFAULT_CRIT_MULTIPLIER, 0f, 0f, 0f, 0f, 0f, Delivery.HIT, false, 0f, 0f);

    /** Never crits and penetrates nothing. */
    public static AttackProfile none() {
        return NONE;
    }

    public static AttackProfile critChance(float critChance) {
        return NONE.withCritChance(critChance);
    }

    public AttackProfile withCritChance(float critChance) {
        return new AttackProfile(critChance, this.critMultiplier, this.armorPenetration, this.armorPenetrationFlat,
                this.magicPenetration, this.magicPenetrationFlat, this.platingPenetration, this.delivery, this.guaranteedCrit,
                this.glancingArmorPenetrationFlat, this.glancingPlatingPenetration);
    }

    public AttackProfile withCritMultiplier(float critMultiplier) {
        return new AttackProfile(this.critChance, critMultiplier, this.armorPenetration, this.armorPenetrationFlat,
                this.magicPenetration, this.magicPenetrationFlat, this.platingPenetration, this.delivery, this.guaranteedCrit,
                this.glancingArmorPenetrationFlat, this.glancingPlatingPenetration);
    }

    public AttackProfile withArmorPenetration(float fraction, float flat) {
        return new AttackProfile(this.critChance, this.critMultiplier, fraction, flat, this.magicPenetration,
                this.magicPenetrationFlat, this.platingPenetration, this.delivery, this.guaranteedCrit,
                this.glancingArmorPenetrationFlat, this.glancingPlatingPenetration);
    }

    public AttackProfile withMagicPenetration(float fraction, float flat) {
        return new AttackProfile(this.critChance, this.critMultiplier, this.armorPenetration,
                this.armorPenetrationFlat, fraction, flat, this.platingPenetration, this.delivery, this.guaranteedCrit,
                this.glancingArmorPenetrationFlat, this.glancingPlatingPenetration);
    }

    public AttackProfile withPlatingPenetration(float fraction) {
        return new AttackProfile(this.critChance, this.critMultiplier, this.armorPenetration,
                this.armorPenetrationFlat, this.magicPenetration, this.magicPenetrationFlat, fraction, this.delivery, this.guaranteedCrit,
                this.glancingArmorPenetrationFlat, this.glancingPlatingPenetration);
    }

    /** The same attack as periodic damage: it never crits. */
    public AttackProfile asPeriodic() {
        return new AttackProfile(this.critChance, this.critMultiplier, this.armorPenetration,
                this.armorPenetrationFlat, this.magicPenetration, this.magicPenetrationFlat, this.platingPenetration,
                Delivery.PERIODIC, this.guaranteedCrit, this.glancingArmorPenetrationFlat, this.glancingPlatingPenetration);
    }

    /** A hit that crits whenever the target is not crit-immune; its size still shrinks with resilience. */
    public AttackProfile withGuaranteedCrit() {
        return new AttackProfile(this.critChance, this.critMultiplier, this.armorPenetration,
                this.armorPenetrationFlat, this.magicPenetration, this.magicPenetrationFlat, this.platingPenetration,
                this.delivery, true, this.glancingArmorPenetrationFlat, this.glancingPlatingPenetration);
    }

    /** Adds {@code bonus} to the crit multiplier, so several sources stack instead of overwriting. */
    public AttackProfile withCritDamageBonus(float bonus) {
        return this.withCritMultiplier(this.critMultiplier + bonus);
    }

    /**
     * A hit that only a glancing blow gets the better of: when it is not critical it ignores
     * {@code armorFlat} more armor or magic resist and {@code platingFraction} of the plating.
     */
    public AttackProfile withGlancingPenetration(float armorFlat, float platingFraction) {
        return new AttackProfile(this.critChance, this.critMultiplier, this.armorPenetration,
                this.armorPenetrationFlat, this.magicPenetration, this.magicPenetrationFlat, this.platingPenetration,
                this.delivery, this.guaranteedCrit, armorFlat, platingFraction);
    }

    /** {@code mitigation} after this attacker's penetration for {@code type}. */
    public float penetrate(DamageType type, float mitigation, boolean critical) {
        if (mitigation <= 0f) {
            return mitigation;
        }
        float fraction = type == DamageType.PHYSICAL ? this.armorPenetration : this.magicPenetration;
        float flat = type == DamageType.PHYSICAL ? this.armorPenetrationFlat : this.magicPenetrationFlat;
        float glancing = critical ? 0f : this.glancingArmorPenetrationFlat;
        return Math.max(0f, mitigation * (1f - fraction) - flat - glancing);
    }

    /** {@code plating} after this attacker's plating penetration. */
    public float penetratePlating(float plating, boolean critical) {
        float fraction = Math.max(this.platingPenetration, critical ? 0f : this.glancingPlatingPenetration);
        return plating * Math.max(0f, 1f - fraction);
    }
}
