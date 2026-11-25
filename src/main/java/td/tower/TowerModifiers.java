package td.tower;

import td.damage.DamageUnits;
import td.stat.StatModifier;
import td.stat.StatModifiers;

/** Tower content's stat changes, one factory per stat; combine them with {@link StatModifiers#plus}. */
public final class TowerModifiers {

    private TowerModifiers() {
    }

    public static StatModifiers<TowerStat> damagePercent(float fraction) {
        return StatModifiers.of(TowerStat.DAMAGE, StatModifier.percent(fraction));
    }

    /** The damage {@code factor} times as big, after every flat and percent bonus. */
    public static StatModifiers<TowerStat> damageTimes(float factor) {
        return StatModifiers.of(TowerStat.DAMAGE, StatModifier.times(factor));
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

    public static StatModifiers<TowerStat> accuracy(float points) {
        return flat(TowerStat.ACCURACY, points);
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

    public static StatModifiers<TowerStat> magicPenetrationFlat(float points) {
        return flat(TowerStat.MAGIC_PENETRATION_FLAT, points);
    }

    public static StatModifiers<TowerStat> flatPenetration(float fraction) {
        return flat(TowerStat.FLAT_PENETRATION, fraction);
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

    public static StatModifiers<TowerStat> damage(float points) {
        return flat(TowerStat.DAMAGE, DamageUnits.ofPoints(points));
    }

    public static StatModifiers<TowerStat> minimumRange(float cells) {
        return flat(TowerStat.MINIMUM_RANGE, cells);
    }

    public static StatModifiers<TowerStat> areaRadius(float cells) {
        return flat(TowerStat.AREA_RADIUS, cells);
    }

    public static StatModifiers<TowerStat> blastEdge(float cells) {
        return flat(TowerStat.BLAST_EDGE, cells);
    }

    public static StatModifiers<TowerStat> projectileSpeed(float fraction) {
        return flat(TowerStat.PROJECTILE_SPEED, fraction);
    }

    public static StatModifiers<TowerStat> projectileSize(float fraction) {
        return flat(TowerStat.PROJECTILE_SIZE, fraction);
    }

    public static StatModifiers<TowerStat> heavyMultiplier(float fraction) {
        return flat(TowerStat.HEAVY_MULTIPLIER, fraction);
    }

    /** The cone {@code factor} times as wide, after every flat and percent bonus. */
    public static StatModifiers<TowerStat> coneTimes(float factor) {
        return StatModifiers.of(TowerStat.CONE_WIDTH, StatModifier.times(factor));
    }

    public static StatModifiers<TowerStat> stream(int ticks) {
        return flat(TowerStat.STREAM, ticks);
    }

    /** The stream {@code factor} times as long, after every flat and percent bonus. */
    public static StatModifiers<TowerStat> streamTimes(float factor) {
        return StatModifiers.of(TowerStat.STREAM, StatModifier.times(factor));
    }

    public static StatModifiers<TowerStat> reload(int ticks) {
        return flat(TowerStat.RELOAD, ticks);
    }

    public static StatModifiers<TowerStat> revealTime(int ticks) {
        return flat(TowerStat.REVEAL_TIME, ticks);
    }

    public static StatModifiers<TowerStat> tollPace(int ticks) {
        return flat(TowerStat.TOLL_PACE, ticks);
    }

    public static StatModifiers<TowerStat> dragPerToll(float share) {
        return flat(TowerStat.DRAG_PER_TOLL, share);
    }

    public static StatModifiers<TowerStat> freezeDuration(int ticks) {
        return flat(TowerStat.FREEZE_DURATION, ticks);
    }

    public static StatModifiers<TowerStat> nestCapacity(int missiles) {
        return flat(TowerStat.NEST_CAPACITY, missiles);
    }

    public static StatModifiers<TowerStat> aimStacks(int stacks) {
        return flat(TowerStat.AIM_STACKS, stacks);
    }

    public static StatModifiers<TowerStat> aimStacksPerShot(int stacks) {
        return flat(TowerStat.AIM_STACKS_PER_SHOT, stacks);
    }

    private static StatModifiers<TowerStat> flat(TowerStat stat, float amount) {
        return StatModifiers.of(stat, StatModifier.flat(amount));
    }
}
