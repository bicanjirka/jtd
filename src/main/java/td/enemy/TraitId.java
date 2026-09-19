package td.enemy;

/**
 * Identifies one composable {@link Trait} or {@link Ability} within a single
 * {@link EnemyDefinition}'s own list, so a later rank (or a spawn shape, e.g. {@code armored})
 * can replace an earlier same-identified entry in place instead of stacking a second one
 * alongside it - see {@link EnemyDefinition#withAdditionalTraits}/
 * {@link EnemyDefinition#withAdditionalAbilities}. Composition is always scoped to one
 * definition's own list, never merged across unrelated enemies, so two different enemies
 * reusing the same {@link #named} id can never collide.
 * <p>
 * {@link #anonymous()} is the default for an ordinary, non-replaceable addition - unique per
 * call, backed by a fresh identity token rather than the trait/ability instance itself, since
 * every built-in {@link Trait}/{@link Ability} implementation is a structurally-equal record and
 * using the instance as its own identity would let two unrelated same-shaped instances collide.
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
