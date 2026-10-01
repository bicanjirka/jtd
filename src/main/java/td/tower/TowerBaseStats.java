package td.tower;

import td.damage.AttackProfile;
import td.damage.DamageUnits;

/** A tower's authored stats before buffs and upgrades; {@link TowerStats} is the current result. */
public record TowerBaseStats(int damage, float range, int coolDownMax, float critChanceBase, float critMultiplier) {

    /**
     * Damage in points. No innate crit chance, and the default crit multiplier should one be bought.
     */
    public TowerBaseStats(float damagePoints, float range, int coolDownMax) {
        this(DamageUnits.ofPoints(damagePoints), range, coolDownMax, 0f, AttackProfile.DEFAULT_CRIT_MULTIPLIER);
    }

    public TowerBaseStats withCritChance(float critChanceBase) {
        return new TowerBaseStats(this.damage, this.range, this.coolDownMax, critChanceBase, this.critMultiplier);
    }

    public TowerBaseStats withCritMultiplier(float critMultiplier) {
        return new TowerBaseStats(this.damage, this.range, this.coolDownMax, this.critChanceBase, critMultiplier);
    }
}
