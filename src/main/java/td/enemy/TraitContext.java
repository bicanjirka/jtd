package td.enemy;

/**
 * What a {@link Trait} needs to compute its result - a definition's traits are shared instances
 * (built once, reused by every mob built from that definition), so any per-mob state a trait's
 * formula needs has to arrive as a parameter at the point the trait actually runs, not be baked
 * into the trait itself.
 */
public record TraitContext(float healthFraction) {
}
