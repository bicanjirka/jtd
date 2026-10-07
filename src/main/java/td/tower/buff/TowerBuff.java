package td.tower.buff;

/**
 * Bonuses on each axis as fractions, 0 meaning none. {@link #none()} is the identity for
 * {@link #combine}, which adds (crit damage adds to a tower's crit multiplier), except fire rate and
 * the disruption shield: each fire-rate bonus cuts the wait that is left, and each shield the disruption that is
 * left, so they multiply and no stack of them removes the whole of either.
 * <p>
 * Start from the axis you care about ({@code TowerBuff.damage(0.3f).withRange(0.1f)}) rather than a
 * positional literal.
 */
public record TowerBuff(float damageBonus, float rangeBonus, float fireRateBonus, float bountyBonus,
                        float critChanceBonus, float armorPenetrationBonus, float magicPenetrationBonus,
                        float critDamageBonus, float disruptionShield, float timedBuffBonus,
                        float effectDurationBonus) {

    /** The lowest combined fire-rate or range bonus any mix of penalties can reach. */
    public static final float MIN_BONUS = -0.75f;

    /** The highest fire-rate bonus: a tower never runs more than ten times faster than its base. */
    public static final float MAX_FIRE_RATE_BONUS = 0.9f;

    private static final TowerBuff NONE = new TowerBuff(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f);

    /** No crit-chance or penetration bonus. */
    public TowerBuff(float damageBonus, float rangeBonus, float fireRateBonus, float bountyBonus) {
        this(damageBonus, rangeBonus, fireRateBonus, bountyBonus, 0f);
    }

    /** No penetration bonus. */
    public TowerBuff(float damageBonus, float rangeBonus, float fireRateBonus, float bountyBonus,
            float critChanceBonus) {
        this(damageBonus, rangeBonus, fireRateBonus, bountyBonus, critChanceBonus, 0f, 0f, 0f, 0f, 0f, 0f);
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

    public static TowerBuff critDamage(float critDamageBonus) {
        return NONE.withCritDamage(critDamageBonus);
    }

    public static TowerBuff armorPenetration(float fraction) {
        return NONE.withArmorPenetration(fraction);
    }

    public static TowerBuff magicPenetration(float fraction) {
        return NONE.withMagicPenetration(fraction);
    }

    public TowerBuff withDamage(float damageBonus) {
        return new TowerBuff(damageBonus, this.rangeBonus, this.fireRateBonus, this.bountyBonus, this.critChanceBonus, this.armorPenetrationBonus, this.magicPenetrationBonus, this.critDamageBonus, this.disruptionShield, this.timedBuffBonus, this.effectDurationBonus);
    }

    public TowerBuff withRange(float rangeBonus) {
        return new TowerBuff(this.damageBonus, rangeBonus, this.fireRateBonus, this.bountyBonus, this.critChanceBonus, this.armorPenetrationBonus, this.magicPenetrationBonus, this.critDamageBonus, this.disruptionShield, this.timedBuffBonus, this.effectDurationBonus);
    }

    public TowerBuff withFireRate(float fireRateBonus) {
        return new TowerBuff(this.damageBonus, this.rangeBonus, fireRateBonus, this.bountyBonus, this.critChanceBonus, this.armorPenetrationBonus, this.magicPenetrationBonus, this.critDamageBonus, this.disruptionShield, this.timedBuffBonus, this.effectDurationBonus);
    }

    public TowerBuff withBounty(float bountyBonus) {
        return new TowerBuff(this.damageBonus, this.rangeBonus, this.fireRateBonus, bountyBonus, this.critChanceBonus, this.armorPenetrationBonus, this.magicPenetrationBonus, this.critDamageBonus, this.disruptionShield, this.timedBuffBonus, this.effectDurationBonus);
    }

    public TowerBuff withCritChance(float critChanceBonus) {
        return new TowerBuff(this.damageBonus, this.rangeBonus, this.fireRateBonus, this.bountyBonus, critChanceBonus, this.armorPenetrationBonus, this.magicPenetrationBonus, this.critDamageBonus, this.disruptionShield, this.timedBuffBonus, this.effectDurationBonus);
    }

    public TowerBuff withArmorPenetration(float armorPenetrationBonus) {
        return new TowerBuff(this.damageBonus, this.rangeBonus, this.fireRateBonus, this.bountyBonus, this.critChanceBonus, armorPenetrationBonus, this.magicPenetrationBonus, this.critDamageBonus, this.disruptionShield, this.timedBuffBonus, this.effectDurationBonus);
    }

    public TowerBuff withMagicPenetration(float magicPenetrationBonus) {
        return new TowerBuff(this.damageBonus, this.rangeBonus, this.fireRateBonus, this.bountyBonus, this.critChanceBonus, this.armorPenetrationBonus, magicPenetrationBonus, this.critDamageBonus, this.disruptionShield, this.timedBuffBonus, this.effectDurationBonus);
    }

    public TowerBuff withCritDamage(float critDamageBonus) {
        return new TowerBuff(this.damageBonus, this.rangeBonus, this.fireRateBonus, this.bountyBonus, this.critChanceBonus, this.armorPenetrationBonus, this.magicPenetrationBonus, critDamageBonus, this.disruptionShield, this.timedBuffBonus, this.effectDurationBonus);
    }

    public TowerBuff withDisruptionShield(float disruptionShield) {
        return new TowerBuff(this.damageBonus, this.rangeBonus, this.fireRateBonus, this.bountyBonus, this.critChanceBonus, this.armorPenetrationBonus, this.magicPenetrationBonus, this.critDamageBonus, disruptionShield, this.timedBuffBonus, this.effectDurationBonus);
    }

    /** How much longer the tower's own timed buffs last: {@code 1} doubles them. */
    public TowerBuff withTimedBuffBonus(float timedBuffBonus) {
        return new TowerBuff(this.damageBonus, this.rangeBonus, this.fireRateBonus, this.bountyBonus, this.critChanceBonus, this.armorPenetrationBonus, this.magicPenetrationBonus, this.critDamageBonus, this.disruptionShield, timedBuffBonus, this.effectDurationBonus);
    }

    /** How much longer the effects the tower puts on enemies last. */
    public TowerBuff withEffectDurationBonus(float effectDurationBonus) {
        return new TowerBuff(this.damageBonus, this.rangeBonus, this.fireRateBonus, this.bountyBonus, this.critChanceBonus, this.armorPenetrationBonus, this.magicPenetrationBonus, this.critDamageBonus, this.disruptionShield, this.timedBuffBonus, effectDurationBonus);
    }

    public static TowerBuff disruptionShield(float fraction) {
        return NONE.withDisruptionShield(fraction);
    }

    public TowerBuff combine(TowerBuff other) {
        return new TowerBuff(
                this.damageBonus + other.damageBonus,
                this.rangeBonus + other.rangeBonus,
                this.fireRateBonus + other.fireRateBonus - this.fireRateBonus * other.fireRateBonus,
                this.bountyBonus + other.bountyBonus,
                this.critChanceBonus + other.critChanceBonus,
                this.armorPenetrationBonus + other.armorPenetrationBonus,
                this.magicPenetrationBonus + other.magicPenetrationBonus,
                this.critDamageBonus + other.critDamageBonus,
                1f - (1f - this.disruptionShield) * (1f - other.disruptionShield),
                this.timedBuffBonus + other.timedBuffBonus,
                this.effectDurationBonus + other.effectDurationBonus);
    }

    /** The share of a disruption that still reaches the tower: {@code 0.5} after one half-shield, a quarter after two. */
    public float disruptionLeft() {
        return 1f - Math.min(1f, Math.max(0f, this.disruptionShield));
    }

    /** The factor on the length of the tower's own timed buffs. */
    public float timedBuffLength() {
        return 1f + Math.max(0f, this.timedBuffBonus);
    }

    /** The factor on the length of the effects the tower puts on enemies. */
    public float effectLength() {
        return 1f + Math.max(0f, this.effectDurationBonus);
    }

    public int damageFor(int base) {
        return Math.round(base * (1f + this.damageBonus));
    }

    /** The range grown by the range bonus, which never goes below {@link #MIN_BONUS}. */
    public float rangeFor(float base) {
        return base * (1f + Math.max(MIN_BONUS, this.rangeBonus));
    }

    /**
     * How many times faster than its base cadence a tower runs: the fire-rate bonus cuts the time
     * between shots by that fraction, so {@code 0.5} is twice as fast. The bonus is clamped to
     * {@link #MIN_BONUS} (no stack of penalties stalls a tower) and {@link #MAX_FIRE_RATE_BONUS}.
     */
    public double fireRateMultiplier() {
        double bonus = Math.min(MAX_FIRE_RATE_BONUS, Math.max(MIN_BONUS, this.fireRateBonus));
        return 1.0 / (1.0 - bonus);
    }

    /** {@code base} plus the crit bonus, clamped to a probability. */
    public float critChanceFor(float base) {
        return Math.max(0f, Math.min(1f, base + this.critChanceBonus));
    }
}
