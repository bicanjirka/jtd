package td.enemy;

/**
 * Pairs a {@link Trait} with the {@link TraitId} identity mechanism
 * {@link EnemyDefinition#withAdditionalTraits} composes by - see {@link TraitId}'s own doc
 * comment for why identity lives here rather than on {@link Trait} itself.
 */
public record IdentifiedTrait(TraitId id, Trait trait) {

    public static IdentifiedTrait anonymous(Trait trait) {
        return new IdentifiedTrait(TraitId.anonymous(), trait);
    }

    public static IdentifiedTrait named(String name, Trait trait) {
        return new IdentifiedTrait(TraitId.named(name), trait);
    }
}
