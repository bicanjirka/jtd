package td.tower;

import td.stat.StatModifier;
import td.stat.StatModifiers;

/** Tower content's stat changes, one factory per stat; combine them with {@link StatModifiers#plus}. */
public final class TowerModifiers {

    private TowerModifiers() {
    }

    public static StatModifiers<TowerStat> damagePercent(float fraction) {
        return StatModifiers.of(TowerStat.DAMAGE, StatModifier.percent(fraction));
    }

    public static StatModifiers<TowerStat> rangePercent(float fraction) {
        return StatModifiers.of(TowerStat.RANGE, StatModifier.percent(fraction));
    }

    public static StatModifiers<TowerStat> range(float cells) {
        return flat(TowerStat.RANGE, cells);
    }

    public static StatModifiers<TowerStat> attackSpeed(float fraction) {
        return flat(TowerStat.ATTACK_SPEED, fraction);
    }

    public static StatModifiers<TowerStat> critChance(float fraction) {
        return flat(TowerStat.CRIT_CHANCE, fraction);
    }

    public static StatModifiers<TowerStat> critMultiplier(float bonus) {
        return flat(TowerStat.CRIT_MULTIPLIER, bonus);
    }

    public static StatModifiers<TowerStat> armorPenetration(float fraction) {
        return flat(TowerStat.ARMOR_PENETRATION, fraction);
    }

    public static StatModifiers<TowerStat> armorPenetrationFlat(float points) {
        return flat(TowerStat.ARMOR_PENETRATION_FLAT, points);
    }

    public static StatModifiers<TowerStat> magicPenetration(float fraction) {
        return flat(TowerStat.MAGIC_PENETRATION, fraction);
    }

    public static StatModifiers<TowerStat> platingPenetration(float fraction) {
        return flat(TowerStat.PLATING_PENETRATION, fraction);
    }

    public static StatModifiers<TowerStat> bounty(float fraction) {
        return flat(TowerStat.BOUNTY, fraction);
    }

    public static StatModifiers<TowerStat> effectDuration(float fraction) {
        return flat(TowerStat.EFFECT_DURATION, fraction);
    }

    public static StatModifiers<TowerStat> buffDuration(float fraction) {
        return flat(TowerStat.BUFF_DURATION, fraction);
    }

    public static StatModifiers<TowerStat> disruptionTaken(float factor) {
        return StatModifiers.of(TowerStat.DISRUPTION_TAKEN, StatModifier.times(factor));
    }

    public static StatModifiers<TowerStat> witherEvery(int ticks) {
        return StatModifiers.of(TowerStat.WITHER_INTERVAL, StatModifier.setTo(ticks));
    }

    private static StatModifiers<TowerStat> flat(TowerStat stat, float amount) {
        return StatModifiers.of(stat, StatModifier.flat(amount));
    }
}
