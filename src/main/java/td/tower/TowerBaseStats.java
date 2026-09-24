package td.tower;

import td.damage.AttackProfile;

/** A tower's authored stats before buffs and upgrades; {@link TowerStats} is the current result. */
public record TowerBaseStats(int damage, float range, int coolDownMax, float critChanceBase, float critMultiplier) {

    /** No innate crit chance, and the default crit multiplier should one be bought. */
    public TowerBaseStats(int damage, float range, int coolDownMax) {
        this(damage, range, coolDownMax, 0f, AttackProfile.DEFAULT_CRIT_MULTIPLIER);
    }

    public TowerBaseStats withCritChance(float critChanceBase) {
        return new TowerBaseStats(this.damage, this.range, this.coolDownMax, critChanceBase, this.critMultiplier);
    }

    public TowerBaseStats withCritMultiplier(float critMultiplier) {
        return new TowerBaseStats(this.damage, this.range, this.coolDownMax, this.critChanceBase, critMultiplier);
    }
}
