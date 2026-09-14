package td.enemy;

/**
 * What a {@link Trait} needs to compute a level-scaled result - a definition's traits are
 * shared, level-blind instances (built once, reused by every mob built from that definition
 * regardless of which wave's level spawned it), so level has to arrive as a parameter at the
 * point a trait actually runs, not be baked into the trait itself.
 */
public record TraitContext(int level, float healthFraction) {
}
