package td.tower;

/**
 * A leaf tower's authored, unbuffed stats - what it costs to fire before any Aura tower or
 * upgrade path touches it. Distinct from {@link TowerStats}, which is the current, buffed
 * snapshot {@code AbstractTower} recomputes on every roster/path change; this is the fixed
 * input that recomputation always folds a buff on top of.
 */
public record TowerBaseStats(int damage, float range, int coolDownMax, float critChanceBase) {

    /**
     * Equivalent to the four-argument canonical constructor with {@code critChanceBase = 0} -
     * every tower except {@code SniperTower} has no innate crit chance of its own.
     */
    public TowerBaseStats(int damage, float range, int coolDownMax) {
        this(damage, range, coolDownMax, 0f);
    }

    public TowerBaseStats withCritChance(float critChanceBase) {
        return new TowerBaseStats(this.damage, this.range, this.coolDownMax, critChanceBase);
    }
}
