package td.enemy;

/** An {@link Ability} with the {@link TraitId} it is composed by. */
public record IdentifiedAbility(TraitId id, Ability ability) {

    public static IdentifiedAbility anonymous(Ability ability) {
        return new IdentifiedAbility(TraitId.anonymous(), ability);
    }

    public static IdentifiedAbility named(String name, Ability ability) {
        return new IdentifiedAbility(TraitId.named(name), ability);
    }
}
