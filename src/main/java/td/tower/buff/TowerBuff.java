package td.tower.buff;

/**
 * A damage/range/fire-rate/bounty bonus a {@code AuraTower} contributes to a nearby tower,
 * or a tower's own chosen upgrade path contributes to itself, expressed as a fraction
 * (0 = no bonus on that axis). {@link #none()} is the identity element - combining it with
 * any buff returns that buff unchanged - so a tower's total buff is
 * {@code upgTowers.stream().map(AuraTower::buff).reduce(TowerBuff.none(), TowerBuff::combine)}
 * with no size-0/size-1 special case. Combining is additive rather than multiplicative so
 * that upgrades of equal strength stack the same way {@code 1 + power * count} used to.
 */
public record TowerBuff(float damageBonus, float rangeBonus, float fireRateBonus, float bountyBonus) {

    private static final TowerBuff NONE = new TowerBuff(0f, 0f, 0f, 0f);

    public static TowerBuff none() {
        return NONE;
    }

    /**
     * A buff touching only damage and range by the same fraction - what every Aura tower grants.
     */
    public static TowerBuff amplifying(float fraction) {
        return new TowerBuff(fraction, fraction, 0f, 0f);
    }

    public TowerBuff combine(TowerBuff other) {
        return new TowerBuff(
                this.damageBonus + other.damageBonus,
                this.rangeBonus + other.rangeBonus,
                this.fireRateBonus + other.fireRateBonus,
                this.bountyBonus + other.bountyBonus);
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
}
