package td.enemy;

/** A {@link Trait} with the {@link TraitId} it is composed by. */
public record IdentifiedTrait(TraitId id, Trait trait) {

    public static IdentifiedTrait anonymous(Trait trait) {
        return new IdentifiedTrait(TraitId.anonymous(), trait);
    }

    public static IdentifiedTrait named(String name, Trait trait) {
        return new IdentifiedTrait(TraitId.named(name), trait);
    }
}
