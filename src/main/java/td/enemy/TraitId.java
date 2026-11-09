package td.enemy;

/**
 * Identifies a {@link Trait} or {@link Ability} within one definition's list, so a later rank can
 * replace it in place instead of stacking a second. Scoped to one definition, so equal names on
 * different enemies never collide. What an {@link Affix} adds is identified by the affix, which a
 * slot carries at most once, so the enemy's own sheet can leave it to the affix's row.
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

    static TraitId of(Affix affix) {
        return new FromAffix(affix);
    }

    /** Whether an affix added it, rather than the enemy's own type and rank. */
    default boolean fromAffix() {
        return this instanceof FromAffix;
    }

    record Named(String name) implements TraitId {
    }

    record FromAffix(Affix affix) implements TraitId {
    }

    record Anonymous(Object token) implements TraitId {
    }
}
