package td.stat;

/**
 * Where traits and effects put their contributions while a sheet resolves. The primitive forms
 * exist so a value that changes every tick (a slow's recovery curve) needs no modifier object.
 */
public interface StatAccumulator<S extends Enum<S> & StatKey> {

    void add(S stat, StatModifier modifier);

    void multiply(S stat, float factor);

    void addFlat(S stat, float amount);

    /** Holds the stat to at most {@code share} of its base value, whatever else raised it. */
    void capAtShareOfBase(S stat, float share);

}
