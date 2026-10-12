package td.stat;

/**
 * Where traits and effects put their contributions while a sheet resolves. The primitive forms
 * exist so a value that changes every tick (a slow's recovery curve) needs no modifier object.
 */
public interface StatAccumulator<S extends Enum<S> & StatKey> {

    void add(S stat, StatModifier modifier);

    void multiply(S stat, float factor);

    /** A flat amount added to the stat, for one that changes every tick. */
    void addFlat(S stat, float amount);

    /** A flat amount the enemy receives, scaled by its spirit (a heal's regeneration). */
    void restoreFlat(S stat, float amount);

    /** Holds the stat to at most {@code share} of its base value, whatever else raised it. */
    void capAtShareOfBase(S stat, float share);

    /**
     * A fractional reduction the enemy receives, scaled by its spirit (a shield lowering damage
     * taken). Reductions on one stat add up, capped at a full reduction.
     */
    void restoreReduction(S stat, float fraction);
}
