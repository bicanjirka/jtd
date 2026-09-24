package td.tower.buff;

/**
 * Bonuses on each axis as fractions, 0 meaning none. {@link #none()} is the identity for
 * {@link #combine}, which adds, so buffs of equal strength stack linearly.
 * <p>
 * Start from the axis you care about ({@code TowerBuff.damage(0.3f).withRange(0.1f)}) rather than a
 * positional literal.
 */
public record TowerBuff(float damageBonus, float rangeBonus, float fireRateBonus, float bountyBonus,
                        float critChanceBonus) {

    private static final TowerBuff NONE = new TowerBuff(0f, 0f, 0f, 0f, 0f);

    /** No crit-chance bonus. */
    public TowerBuff(float damageBonus, float rangeBonus, float fireRateBonus, float bountyBonus) {
        this(damageBonus, rangeBonus, fireRateBonus, bountyBonus, 0f);
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

    public TowerBuff withDamage(float damageBonus) {
        return new TowerBuff(damageBonus, this.rangeBonus, this.fireRateBonus, this.bountyBonus,
                this.critChanceBonus);
    }

    public TowerBuff withRange(float rangeBonus) {
        return new TowerBuff(this.damageBonus, rangeBonus, this.fireRateBonus, this.bountyBonus,
                this.critChanceBonus);
    }

    public TowerBuff withFireRate(float fireRateBonus) {
        return new TowerBuff(this.damageBonus, this.rangeBonus, fireRateBonus, this.bountyBonus,
                this.critChanceBonus);
    }

    public TowerBuff withBounty(float bountyBonus) {
        return new TowerBuff(this.damageBonus, this.rangeBonus, this.fireRateBonus, bountyBonus,
                this.critChanceBonus);
    }

    public TowerBuff withCritChance(float critChanceBonus) {
        return new TowerBuff(this.damageBonus, this.rangeBonus, this.fireRateBonus, this.bountyBonus,
                critChanceBonus);
    }

    public TowerBuff combine(TowerBuff other) {
        return new TowerBuff(
                this.damageBonus + other.damageBonus,
                this.rangeBonus + other.rangeBonus,
                this.fireRateBonus + other.fireRateBonus,
                this.bountyBonus + other.bountyBonus,
                this.critChanceBonus + other.critChanceBonus);
    }

    public int damageFor(int base) {
        return (int) (base * (1f + this.damageBonus));
    }

    public float rangeFor(float base) {
        return base * (1f + this.rangeBonus);
    }

    /** The cooldown shortened by the fire-rate bonus, never below one tick. */
    public int fireRateFor(int baseCoolDown) {
        return Math.max(1, Math.round(baseCoolDown * (1f - this.fireRateBonus)));
    }

    /** {@code base} plus the crit bonus, clamped to a probability. */
    public float critChanceFor(float base) {
        return Math.max(0f, Math.min(1f, base + this.critChanceBonus));
    }
}
