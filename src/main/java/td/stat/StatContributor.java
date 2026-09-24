package td.stat;

/** Whatever modifies a {@link StatSheet}: it re-adds every current modifier when asked. */
@FunctionalInterface
public interface StatContributor {

    void contributeTo(StatAccumulator accumulator);
}
