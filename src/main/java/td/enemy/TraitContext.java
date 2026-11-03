package td.enemy;

/**
 * Per-mob inputs a shared {@link Trait} instance computes against.
 *
 * @param alliesNearby how many other enemies stand within {@link #ALLY_REACH_CELLS} of it; counted only for a mob
 *                     with a trait that {@link Trait#readsAllies() reads it}, so {@code 0} for every other
 */
public record TraitContext(float healthFraction, int alliesNearby) {

    /** How far an ally may stand and still count as near. */
    public static final float ALLY_REACH_CELLS = 1.5f;

    /** No allies near. */
    public TraitContext(float healthFraction) {
        this(healthFraction, 0);
    }
}
