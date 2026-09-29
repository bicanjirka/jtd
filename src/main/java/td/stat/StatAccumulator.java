package td.stat;

/**
 * Where traits and effects put their contributions while a {@link StatSheet} resolves. The
 * primitive forms exist so a value that changes every tick (a slow's recovery curve) needs no
 * modifier object.
 */
public interface StatAccumulator {

    void add(EnemyStat stat, StatModifier modifier);

    void multiply(EnemyStat stat, float factor);

    /** A flat amount added to the stat, for one that changes every tick. */
    void addFlat(EnemyStat stat, float amount);

    /** A flat amount the enemy receives, scaled by its spirit (a heal's regeneration). */
    void restoreFlat(EnemyStat stat, float amount);

    /**
     * A fractional reduction the enemy receives, scaled by its spirit (a shield lowering damage
     * taken). Reductions on one stat add up, capped at a full reduction.
     */
    void restoreReduction(EnemyStat stat, float fraction);
}
