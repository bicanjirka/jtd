package td.damage;

/**
 * The attacker's side of a hit: crit chance and multiplier, and penetration against armor, magic
 * resist, plating and insulation. It travels with the hit, so whoever lands it (a projectile, a burn tick) never
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
 * @param flatPenetration         fraction of plating and insulation ignored
 * @param delivery                whether this is a hit, which can crit, or periodic damage, which never does
 * @param guaranteedCrit          a hit that crits whenever the target's resilience is below 100
 * @param glancingArmorPenetrationFlat extra armor and magic resist points ignored when the hit is not critical
 * @param glancingFlatPenetration fraction of plating and insulation ignored when the hit is not critical
 * @param critsPierceShields      whether a critical hit goes through a shield to health
 * @param origin                  who attacks; the tower that lands the hit stamps it
 * @param ignoresShields          whether no shield takes a share of the hit
 * @param accuracy                agility points of the target the hit ignores
 * @param aimed                   whether the hit is aimed at one enemy, so that enemy's agility may dodge it
 */
public record AttackProfile(float critChance, float critMultiplier, float armorPenetration,
                            float armorPenetrationFlat, float magicPenetration, float magicPenetrationFlat,
                            float flatPenetration, Delivery delivery, boolean guaranteedCrit,
                            float glancingArmorPenetrationFlat, float glancingFlatPenetration,
                            boolean critsPierceShields, AttackOrigin origin,
                            boolean ignoresShields, float accuracy, boolean aimed) {

    public static final float DEFAULT_CRIT_MULTIPLIER = 1.5f;

    private static final AttackProfile NONE = new AttackProfile(0f, DEFAULT_CRIT_MULTIPLIER, 0f, 0f, 0f, 0f, 0f, Delivery.HIT, false, 0f, 0f, false, AttackOrigin.none(), false, 0f, false);

    /** Never crits and penetrates nothing. */
    public static AttackProfile none() {
        return NONE;
    }

    public static AttackProfile critChance(float critChance) {
        return NONE.withCritChance(critChance);
    }

    public AttackProfile withCritChance(float critChance) {
        return new AttackProfile(critChance, this.critMultiplier, this.armorPenetration, this.armorPenetrationFlat,
                this.magicPenetration, this.magicPenetrationFlat, this.flatPenetration, this.delivery, this.guaranteedCrit,
                this.glancingArmorPenetrationFlat, this.glancingFlatPenetration, this.critsPierceShields, this.origin, this.ignoresShields,
                this.accuracy, this.aimed);
    }

    public AttackProfile withCritMultiplier(float critMultiplier) {
        return new AttackProfile(this.critChance, critMultiplier, this.armorPenetration, this.armorPenetrationFlat,
                this.magicPenetration, this.magicPenetrationFlat, this.flatPenetration, this.delivery, this.guaranteedCrit,
                this.glancingArmorPenetrationFlat, this.glancingFlatPenetration, this.critsPierceShields, this.origin, this.ignoresShields,
                this.accuracy, this.aimed);
    }

    public AttackProfile withArmorPenetration(float fraction, float flat) {
        return new AttackProfile(this.critChance, this.critMultiplier, fraction, flat, this.magicPenetration,
                this.magicPenetrationFlat, this.flatPenetration, this.delivery, this.guaranteedCrit,
                this.glancingArmorPenetrationFlat, this.glancingFlatPenetration, this.critsPierceShields, this.origin, this.ignoresShields,
                this.accuracy, this.aimed);
    }

    public AttackProfile withMagicPenetration(float fraction, float flat) {
        return new AttackProfile(this.critChance, this.critMultiplier, this.armorPenetration,
                this.armorPenetrationFlat, fraction, flat, this.flatPenetration, this.delivery, this.guaranteedCrit,
                this.glancingArmorPenetrationFlat, this.glancingFlatPenetration, this.critsPierceShields, this.origin, this.ignoresShields,
                this.accuracy, this.aimed);
    }

    public AttackProfile withFlatPenetration(float fraction) {
        return new AttackProfile(this.critChance, this.critMultiplier, this.armorPenetration,
                this.armorPenetrationFlat, this.magicPenetration, this.magicPenetrationFlat, fraction, this.delivery, this.guaranteedCrit,
                this.glancingArmorPenetrationFlat, this.glancingFlatPenetration, this.critsPierceShields, this.origin, this.ignoresShields,
                this.accuracy, this.aimed);
    }

    /** The same attack as periodic damage: it never crits. */
    public AttackProfile asPeriodic() {
        return new AttackProfile(this.critChance, this.critMultiplier, this.armorPenetration,
                this.armorPenetrationFlat, this.magicPenetration, this.magicPenetrationFlat, this.flatPenetration,
                Delivery.PERIODIC, this.guaranteedCrit, this.glancingArmorPenetrationFlat, this.glancingFlatPenetration, this.critsPierceShields, this.origin, this.ignoresShields,
                this.accuracy, this.aimed);
    }

    /** A hit that crits whenever the target is not crit-immune; its size still shrinks with resilience. */
    public AttackProfile withGuaranteedCrit() {
        return new AttackProfile(this.critChance, this.critMultiplier, this.armorPenetration,
                this.armorPenetrationFlat, this.magicPenetration, this.magicPenetrationFlat, this.flatPenetration,
                this.delivery, true, this.glancingArmorPenetrationFlat, this.glancingFlatPenetration, this.critsPierceShields, this.origin, this.ignoresShields,
                this.accuracy, this.aimed);
    }

    /** Adds {@code bonus} to the crit multiplier, so several sources stack instead of overwriting. */
    public AttackProfile withCritDamageBonus(float bonus) {
        return this.withCritMultiplier(this.critMultiplier + bonus);
    }

    /**
     * A hit that only a glancing blow gets the better of: when it is not critical it ignores
     * {@code armorFlat} more armor or magic resist and {@code flatFraction} of the plating or insulation.
     */
    public AttackProfile withGlancingPenetration(float armorFlat, float flatFraction) {
        return new AttackProfile(this.critChance, this.critMultiplier, this.armorPenetration,
                this.armorPenetrationFlat, this.magicPenetration, this.magicPenetrationFlat, this.flatPenetration,
                this.delivery, this.guaranteedCrit, armorFlat, flatFraction, this.critsPierceShields, this.origin, this.ignoresShields,
                this.accuracy, this.aimed);
    }

    /** The same attack, from {@code origin}. */
    public AttackProfile withOrigin(AttackOrigin origin) {
        return new AttackProfile(this.critChance, this.critMultiplier, this.armorPenetration,
                this.armorPenetrationFlat, this.magicPenetration, this.magicPenetrationFlat, this.flatPenetration,
                this.delivery, this.guaranteedCrit, this.glancingArmorPenetrationFlat,
                this.glancingFlatPenetration, this.critsPierceShields, origin, this.ignoresShields, this.accuracy, this.aimed);
    }

    /** A critical hit from this attack goes through a shield to health. */
    public AttackProfile withCritsPierceShields() {
        return new AttackProfile(this.critChance, this.critMultiplier, this.armorPenetration,
                this.armorPenetrationFlat, this.magicPenetration, this.magicPenetrationFlat, this.flatPenetration,
                this.delivery, this.guaranteedCrit, this.glancingArmorPenetrationFlat,
                this.glancingFlatPenetration, true, this.origin, this.ignoresShields,
                this.accuracy, this.aimed);
    }

    /** A hit that no shield takes a share of. */
    public AttackProfile withIgnoredShields() {
        return new AttackProfile(this.critChance, this.critMultiplier, this.armorPenetration,
                this.armorPenetrationFlat, this.magicPenetration, this.magicPenetrationFlat, this.flatPenetration,
                this.delivery, this.guaranteedCrit, this.glancingArmorPenetrationFlat,
                this.glancingFlatPenetration, this.critsPierceShields, this.origin, true, this.accuracy, this.aimed);
    }

    public AttackProfile withAccuracy(float accuracy) {
        return new AttackProfile(this.critChance, this.critMultiplier, this.armorPenetration,
                this.armorPenetrationFlat, this.magicPenetration, this.magicPenetrationFlat, this.flatPenetration,
                this.delivery, this.guaranteedCrit, this.glancingArmorPenetrationFlat,
                this.glancingFlatPenetration, this.critsPierceShields, this.origin, this.ignoresShields, accuracy,
                this.aimed);
    }

    /** The same hit aimed at one enemy, which that enemy's agility may dodge. */
    public AttackProfile asAimed() {
        return new AttackProfile(this.critChance, this.critMultiplier, this.armorPenetration,
                this.armorPenetrationFlat, this.magicPenetration, this.magicPenetrationFlat, this.flatPenetration,
                this.delivery, this.guaranteedCrit, this.glancingArmorPenetrationFlat,
                this.glancingFlatPenetration, this.critsPierceShields, this.origin, this.ignoresShields,
                this.accuracy, true);
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

    /** {@code reduction}, a plating or insulation, after this attacker's flat penetration. */
    public float penetrateFlatReduction(float reduction, boolean critical) {
        float fraction = Math.max(this.flatPenetration, critical ? 0f : this.glancingFlatPenetration);
        return reduction * Math.max(0f, 1f - fraction);
    }
}
