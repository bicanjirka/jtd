package td.enemy;

/**
 * Identifies a {@link Trait} or {@link Ability} within one definition's list, so a later rank or a
 * spawn shape can replace it in place instead of stacking a second. Scoped to one definition, so
 * equal names on different enemies never collide.
 * <p>
 * {@link #anonymous()} is unique per call rather than based on the instance, since equal-shaped
 * records would otherwise collide.
 */
public sealed interface TraitId {

    static TraitId named(String name) {
        return new Named(name);
    }

    static TraitId anonymous() {
        return new Anonymous(new Object());
    }

    record Named(String name) implements TraitId {
    }

    record Anonymous(Object token) implements TraitId {
    }
}
