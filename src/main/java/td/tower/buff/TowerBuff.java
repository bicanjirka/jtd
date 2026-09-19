package td.tower.buff;

/**
 * A damage/range/fire-rate/bounty/crit-chance bonus a {@code AuraTower} contributes to a nearby
 * tower, or a tower's own chosen upgrade path contributes to itself, expressed as a fraction
 * (0 = no bonus on that axis). {@link #none()} is the identity element - combining it with
 * any buff returns that buff unchanged - so a tower's total buff is
 * {@code upgTowers.stream().map(AuraTower::buff).reduce(TowerBuff.none(), TowerBuff::combine)}
 * with no size-0/size-1 special case. Combining is additive rather than multiplicative so
 * that upgrades of equal strength stack the same way {@code 1 + power * count} used to.
 * <p>
 * A call site that means to name one or two axes starts the chain from whichever axis it cares
 * about first, through a static entry point named for that axis (e.g.
 * {@code TowerBuff.damage(0.3f).withRange(0.1f)}), then continues with the fluent instance
 * {@code withX} copies for any further axis - instead of a positional literal that has to spell
 * out every axis to reach the ones it cares about, or seeding the chain from {@link #none()}. A
 * sixth axis, were one ever added, would cost these call sites no edits.
 */
public record TowerBuff(float damageBonus, float rangeBonus, float fireRateBonus, float bountyBonus,
                        float critChanceBonus) {

    private static final TowerBuff NONE = new TowerBuff(0f, 0f, 0f, 0f, 0f);

    /**
     * Equivalent to the five-argument canonical constructor with {@code critChanceBonus = 0} -
     * every pre-existing four-argument construction path (this record's original shape, before
     * crit chance existed) stays at no crit bonus without needing to change.
     */
    public TowerBuff(float damageBonus, float rangeBonus, float fireRateBonus, float bountyBonus) {
        this(damageBonus, rangeBonus, fireRateBonus, bountyBonus, 0f);
    }

    public static TowerBuff none() {
        return NONE;
    }

    /**
     * A buff touching only damage and range by the same fraction - what every Aura tower grants.
     */
    public static TowerBuff amplifying(float fraction) {
        return new TowerBuff(fraction, fraction, 0f, 0f);
    }

    /**
     * Starts a fluent chain naming damage first, equivalent to {@code none().withDamage(...)}.
     */
    public static TowerBuff damage(float damageBonus) {
        return NONE.withDamage(damageBonus);
    }

    /**
     * Starts a fluent chain naming range first, equivalent to {@code none().withRange(...)}.
     */
    public static TowerBuff range(float rangeBonus) {
        return NONE.withRange(rangeBonus);
    }

    /**
     * Starts a fluent chain naming fire rate first, equivalent to {@code none().withFireRate(...)}.
     */
    public static TowerBuff fireRate(float fireRateBonus) {
        return NONE.withFireRate(fireRateBonus);
    }

    /**
     * Starts a fluent chain naming bounty first, equivalent to {@code none().withBounty(...)}.
     */
    public static TowerBuff bounty(float bountyBonus) {
        return NONE.withBounty(bountyBonus);
    }

    /**
     * Starts a fluent chain naming crit chance first, equivalent to {@code none().withCritChance(...)}.
     */
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

    /**
     * The base cooldown shortened by {@code fireRateBonus}, floored at one tick so a tower
     * can never end up firing more than once per tick.
     */
    public int fireRateFor(int baseCoolDown) {
        return Math.max(1, Math.round(baseCoolDown * (1f - this.fireRateBonus)));
    }

    /**
     * {@code base} plus this buff's crit-chance bonus, clamped to a valid probability - every
     * tower's base crit chance is {@code 0} today (see {@code TowerStats}), so in practice this
     * is just the bonus itself, but the base parameter mirrors {@link #rangeFor}/{@link #damageFor}
     * rather than assuming that will always stay true.
     */
    public float critChanceFor(float base) {
        return Math.max(0f, Math.min(1f, base + this.critChanceBonus));
    }
}
