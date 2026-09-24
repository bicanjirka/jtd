package td.tower.buff;

/**
 * Bonuses on each axis as fractions, 0 meaning none. {@link #none()} is the identity for
 * {@link #combine}, which adds, so buffs of equal strength stack linearly.
 * <p>
 * Start from the axis you care about ({@code TowerBuff.damage(0.3f).withRange(0.1f)}) rather than a
 * positional literal.
 */
public record TowerBuff(float damageBonus, float rangeBonus, float fireRateBonus, float bountyBonus,
                        float critChanceBonus, float armorPenetrationBonus, float magicPenetrationBonus) {

    /** The lowest combined fire-rate or range bonus any mix of penalties can reach. */
    public static final float MIN_BONUS = -0.75f;

    private static final TowerBuff NONE = new TowerBuff(0f, 0f, 0f, 0f, 0f, 0f, 0f);

    /** No crit-chance or penetration bonus. */
    public TowerBuff(float damageBonus, float rangeBonus, float fireRateBonus, float bountyBonus) {
        this(damageBonus, rangeBonus, fireRateBonus, bountyBonus, 0f);
    }

    /** No penetration bonus. */
    public TowerBuff(float damageBonus, float rangeBonus, float fireRateBonus, float bountyBonus,
            float critChanceBonus) {
        this(damageBonus, rangeBonus, fireRateBonus, bountyBonus, critChanceBonus, 0f, 0f);
    }

    public static TowerBuff none() {
        return NONE;
    }

    /** Damage and range by the same fraction. */
    public static TowerBuff amplifying(float fraction) {
        return new TowerBuff(fraction, fraction, 0f, 0f);
    }

    public static TowerBuff damage(float damageBonus) {
        return NONE.withDamage(damageBonus);
    }

    public static TowerBuff range(float rangeBonus) {
        return NONE.withRange(rangeBonus);
    }

    public static TowerBuff fireRate(float fireRateBonus) {
        return NONE.withFireRate(fireRateBonus);
    }

    public static TowerBuff bounty(float bountyBonus) {
        return NONE.withBounty(bountyBonus);
    }

    public static TowerBuff critChance(float critChanceBonus) {
        return NONE.withCritChance(critChanceBonus);
    }

    public static TowerBuff armorPenetration(float fraction) {
        return NONE.withArmorPenetration(fraction);
    }

    public static TowerBuff magicPenetration(float fraction) {
        return NONE.withMagicPenetration(fraction);
    }

    public TowerBuff withDamage(float damageBonus) {
        return new TowerBuff(damageBonus, this.rangeBonus, this.fireRateBonus, this.bountyBonus, this.critChanceBonus, this.armorPenetrationBonus, this.magicPenetrationBonus);
    }

    public TowerBuff withRange(float rangeBonus) {
        return new TowerBuff(this.damageBonus, rangeBonus, this.fireRateBonus, this.bountyBonus, this.critChanceBonus, this.armorPenetrationBonus, this.magicPenetrationBonus);
    }

    public TowerBuff withFireRate(float fireRateBonus) {
        return new TowerBuff(this.damageBonus, this.rangeBonus, fireRateBonus, this.bountyBonus, this.critChanceBonus, this.armorPenetrationBonus, this.magicPenetrationBonus);
    }

    public TowerBuff withBounty(float bountyBonus) {
        return new TowerBuff(this.damageBonus, this.rangeBonus, this.fireRateBonus, bountyBonus, this.critChanceBonus, this.armorPenetrationBonus, this.magicPenetrationBonus);
    }

    public TowerBuff withCritChance(float critChanceBonus) {
        return new TowerBuff(this.damageBonus, this.rangeBonus, this.fireRateBonus, this.bountyBonus, critChanceBonus, this.armorPenetrationBonus, this.magicPenetrationBonus);
    }

    public TowerBuff withArmorPenetration(float armorPenetrationBonus) {
        return new TowerBuff(this.damageBonus, this.rangeBonus, this.fireRateBonus, this.bountyBonus, this.critChanceBonus, armorPenetrationBonus, this.magicPenetrationBonus);
    }

    public TowerBuff withMagicPenetration(float magicPenetrationBonus) {
        return new TowerBuff(this.damageBonus, this.rangeBonus, this.fireRateBonus, this.bountyBonus, this.critChanceBonus, this.armorPenetrationBonus, magicPenetrationBonus);
    }

    public TowerBuff combine(TowerBuff other) {
        return new TowerBuff(
                this.damageBonus + other.damageBonus,
                this.rangeBonus + other.rangeBonus,
                this.fireRateBonus + other.fireRateBonus,
                this.bountyBonus + other.bountyBonus,
                this.critChanceBonus + other.critChanceBonus,
                this.armorPenetrationBonus + other.armorPenetrationBonus,
                this.magicPenetrationBonus + other.magicPenetrationBonus);
    }

    public int damageFor(int base) {
        return (int) (base * (1f + this.damageBonus));
    }

    /** The range grown by the range bonus, which never goes below {@link #MIN_BONUS}. */
    public float rangeFor(float base) {
        return base * (1f + Math.max(MIN_BONUS, this.rangeBonus));
    }

    /**
     * The cooldown shortened by the fire-rate bonus, never below one tick. The bonus never goes
     * below {@link #MIN_BONUS}, so no stack of penalties stalls a tower completely.
     */
    public int fireRateFor(int baseCoolDown) {
        return Math.max(1, Math.round(baseCoolDown * (1f - Math.max(MIN_BONUS, this.fireRateBonus))));
    }

    /** {@code base} plus the crit bonus, clamped to a probability. */
    public float critChanceFor(float base) {
        return Math.max(0f, Math.min(1f, base + this.critChanceBonus));
    }
}
