package td.enemy;

/** A {@link TraitTemplate} with the {@link TraitId} it is composed by. */
public record IdentifiedTrait(TraitId id, TraitTemplate template) {

    public static IdentifiedTrait anonymous(TraitTemplate template) {
        return new IdentifiedTrait(TraitId.anonymous(), template);
    }

    /** What {@code affix} adds, stacking with every trait the enemy has of its own. */
    public static IdentifiedTrait of(Affix affix, TraitTemplate template) {
        return new IdentifiedTrait(TraitId.of(affix), template);
    }

    public static IdentifiedTrait named(String name, TraitTemplate template) {
        return new IdentifiedTrait(TraitId.named(name), template);
    }
}
