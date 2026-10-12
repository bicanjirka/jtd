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

    public static StatModifiers<TowerStat> damage(float points) {
        return flat(TowerStat.DAMAGE, DamageUnits.ofPoints(points));
    }

    public static StatModifiers<TowerStat> deadZone(float cells) {
        return flat(TowerStat.DEAD_ZONE, cells);
    }

    public static StatModifiers<TowerStat> areaRadius(float cells) {
        return flat(TowerStat.AREA_RADIUS, cells);
    }

    public static StatModifiers<TowerStat> blastEdge(float fraction) {
        return flat(TowerStat.BLAST_EDGE, fraction);
    }

    public static StatModifiers<TowerStat> saturationCap(int stacks) {
        return flat(TowerStat.SATURATION_CAP, stacks);
    }

    public static StatModifiers<TowerStat> saturationInnerStacks(int stacks) {
        return flat(TowerStat.SATURATION_INNER_STACKS, stacks);
    }

    public static StatModifiers<TowerStat> projectileSpeed(float fraction) {
        return flat(TowerStat.PROJECTILE_SPEED, fraction);
    }

    public static StatModifiers<TowerStat> projectileSize(float fraction) {
        return flat(TowerStat.PROJECTILE_SIZE, fraction);
    }

    public static StatModifiers<TowerStat> bracketDamageStep(float fraction) {
        return flat(TowerStat.BRACKET_DAMAGE_STEP, fraction);
    }

    public static StatModifiers<TowerStat> coneWidth(float radians) {
        return flat(TowerStat.CONE_WIDTH, radians);
    }

    public static StatModifiers<TowerStat> waveSpeed(float fraction) {
        return flat(TowerStat.WAVE_SPEED, fraction);
    }

    public static StatModifiers<TowerStat> burnDuration(int ticks) {
        return flat(TowerStat.BURN_DURATION, ticks);
    }

    public static StatModifiers<TowerStat> stokeStep(float fraction) {
        return flat(TowerStat.STOKE_STEP, fraction);
    }

    public static StatModifiers<TowerStat> stokeGrace(int ticks) {
        return flat(TowerStat.STOKE_GRACE, ticks);
    }

    public static StatModifiers<TowerStat> burnPoolCap(float multiple) {
        return flat(TowerStat.BURN_POOL_CAP, multiple);
    }

    public static StatModifiers<TowerStat> burnMarkRate(float multiple) {
        return flat(TowerStat.BURN_MARK_RATE, multiple);
    }

    public static StatModifiers<TowerStat> tollCap(int stacks) {
        return flat(TowerStat.TOLL_CAP, stacks);
    }

    public static StatModifiers<TowerStat> tollRate(float stacksPerSecond) {
        return flat(TowerStat.TOLL_RATE, stacksPerSecond);
    }

    public static StatModifiers<TowerStat> tollLinger(int ticks) {
        return flat(TowerStat.TOLL_LINGER, ticks);
    }

    public static StatModifiers<TowerStat> freezeDuration(int ticks) {
        return flat(TowerStat.FREEZE_DURATION, ticks);
    }

    public static StatModifiers<TowerStat> nestCapacity(int missiles) {
        return flat(TowerStat.NEST_CAPACITY, missiles);
    }

    public static StatModifiers<TowerStat> missilesPerLaunch(int missiles) {
        return flat(TowerStat.MISSILES_PER_LAUNCH, missiles);
    }

    public static StatModifiers<TowerStat> pingCount(int pings) {
        return flat(TowerStat.PING_COUNT, pings);
    }

    public static StatModifiers<TowerStat> aimStacks(int stacks) {
        return flat(TowerStat.AIM_STACKS, stacks);
    }

    public static StatModifiers<TowerStat> auraPower(float fraction) {
        return flat(TowerStat.AURA_POWER, fraction);
    }

    private static StatModifiers<TowerStat> flat(TowerStat stat, float amount) {
        return StatModifiers.of(stat, StatModifier.flat(amount));
    }
}
