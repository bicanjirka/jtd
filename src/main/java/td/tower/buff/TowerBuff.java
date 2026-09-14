package td.tower.buff;

/**
 * A damage/range bonus a {@code TowerAura} contributes to a nearby tower, expressed as
 * a fraction (0 = no buff). {@link #none()} is the identity element - combining it with any
 * buff returns that buff unchanged - so a tower's total buff is
 * {@code upgTowers.stream().map(TowerAura::buff).reduce(TowerBuff.none(), TowerBuff::combine)}
 * with no size-0/size-1 special case. Combining is additive rather than multiplicative so
 * that upgrades of equal strength stack the same way {@code 1 + power * count} used to.
 */
public record TowerBuff(float damageBonus, float rangeBonus) {

    private static final TowerBuff NONE = new TowerBuff(0f, 0f);

    public static TowerBuff none() {
        return NONE;
    }

    public static TowerBuff amplifying(float fraction) {
        return new TowerBuff(fraction, fraction);
    }

    public TowerBuff combine(TowerBuff other) {
        return new TowerBuff(this.damageBonus + other.damageBonus, this.rangeBonus + other.rangeBonus);
    }

    public int damageFor(int base) {
        return (int) (base * (1f + this.damageBonus));
    }

    public float rangeFor(float base) {
        return base * (1f + this.rangeBonus);
    }
}
