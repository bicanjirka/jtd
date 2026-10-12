package td.stat;

/** Whatever modifies a stat sheet: it re-adds every current modifier when asked. */
@FunctionalInterface
public interface StatContributor<S extends Enum<S> & StatKey> {

    void contributeTo(StatAccumulator<S> accumulator);
}
