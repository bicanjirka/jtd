package td.tower;

import td.damage.DamageUnits;
import td.stat.BaseStats;

/** A tower's authored stats before buffs and upgrades; {@link TowerStats} is the current result. */
public record TowerBaseStats(BaseStats<TowerStat> stats, int coolDownMax) {

    /** Damage in points. No innate crit chance, and the default crit multiplier should one be bought. */
    public TowerBaseStats(float damagePoints, float range, int coolDownMax) {
        this(BaseStats.defaults(TowerStat.class).with(TowerStat.DAMAGE, DamageUnits.ofPoints(damagePoints))
                .with(TowerStat.RANGE, range), coolDownMax);
    }

    public TowerBaseStats withCritChance(float critChanceBase) {
        return this.with(TowerStat.CRIT_CHANCE, critChanceBase);
    }

    public TowerBaseStats withCritMultiplier(float critMultiplier) {
        return this.with(TowerStat.CRIT_MULTIPLIER, critMultiplier);
    }

    public TowerBaseStats with(TowerStat stat, float value) {
        return new TowerBaseStats(this.stats.with(stat, value), this.coolDownMax);
    }

    public float value(TowerStat stat) {
        return this.stats.value(stat);
    }

    /** Damage per hit, in hundredths. */
    public int damage() {
        return Math.round(this.value(TowerStat.DAMAGE));
    }

    public float range() {
        return this.value(TowerStat.RANGE);
    }

    public float critChanceBase() {
        return this.value(TowerStat.CRIT_CHANCE);
    }

    public float critMultiplier() {
        return this.value(TowerStat.CRIT_MULTIPLIER);
    }
}
