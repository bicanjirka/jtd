package td.tower;

/** A tower's authored stats before buffs and upgrades; {@link TowerStats} is the current result. */
public record TowerBaseStats(int damage, float range, int coolDownMax, float critChanceBase) {

    /** No innate crit chance. */
    public TowerBaseStats(int damage, float range, int coolDownMax) {
        this(damage, range, coolDownMax, 0f);
    }

    public TowerBaseStats withCritChance(float critChanceBase) {
        return new TowerBaseStats(this.damage, this.range, this.coolDownMax, critChanceBase);
    }
}
